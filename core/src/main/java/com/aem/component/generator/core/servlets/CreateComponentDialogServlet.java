/*
 * Copyright Sumanta Pakira
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.aem.component.generator.core.servlets;

import com.aem.component.generator.core.commons.Constants;
import com.aem.component.generator.core.commons.Utils;
import com.aem.component.generator.core.commons.WorkerThread;
import com.aem.component.generator.core.models.HTLModel;
import com.aem.component.generator.core.models.ReactModel;
import com.aem.component.generator.core.models.SlingModel;
import com.drew.lang.annotations.NotNull;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component(service = Servlet.class, property = {"sling.servlet.methods=" + HttpConstants.METHOD_POST,
        "sling.servlet.paths=" + "/bin/servets/createcomponentdialog", "sling.servlet.extensions=json"})

public class CreateComponentDialogServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(CreateComponentDialogServlet.class);

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    @Reference
    ComponentListService componentListService;

    /**
     * @param componentName
     * @param groupName
     * @param resourceSuperType
     * @param isContainer
     * @param filePath
     */
    private static void createComponentXml(@NotNull String componentName, String groupName, String resourceSuperType, String isContainer, @NotNull String filePath) {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element rootElement = document.createElement("jcr:root");
            rootElement.setAttribute("xmlns:sling", "http://sling.apache.org/jcr/sling/1.0");
            rootElement.setAttribute("xmlns:cq", "http://www.day.com/jcr/cq/1.0");
            rootElement.setAttribute("xmlns:jcr", "http://www.jcp.org/jcr/1.0");
            rootElement.setAttribute("xmlns:nt", "http://www.jcp.org/jcr/nt/1.0");

            rootElement.setAttribute(JcrConstants.JCR_PRIMARYTYPE, "cq:Component");
            rootElement.setAttribute(com.day.cq.commons.jcr.JcrConstants.JCR_TITLE, componentName);
            if (StringUtils.isNotBlank(groupName)) {
                rootElement.setAttribute("componentGroup", groupName);
            }
            if (StringUtils.isNotBlank(resourceSuperType)) {
                rootElement.setAttribute("sling:resourceSuperType", resourceSuperType);
            }
            if (StringUtils.isNotBlank(isContainer) && Boolean.parseBoolean(isContainer)) {
                rootElement.setAttribute("cq:isContainer", "{Boolean}true");
            }
            document.appendChild(rootElement);
            Utils.transformDomToFile(document, filePath);

        } catch (ParserConfigurationException e) {
            LOG.error("Error:", e);
        }
    }

    @Override
    protected void doPost(final SlingHttpServletRequest req,
                          final SlingHttpServletResponse resp) throws ServletException, IOException {
        final Resource resource = req.getResource();
        RequestParameterMap requestParameterMap = req.getRequestParameterMap();
        Map<String, String> unsrotedMap =
                new HashMap<String, String>();
        String componentPath = StringUtils.EMPTY;
        try {
            for (String key : requestParameterMap.keySet()) {
                RequestParameter requestParameter = requestParameterMap.getValue(key);
                String propertyValue = requestParameter.getString();
                if (key.equals("componentPath")) {
                    componentPath = propertyValue;
                }
                unsrotedMap.put(key, propertyValue);
            }

            final NavigableMap<String, String> sortedMap = new TreeMap<>((a, b) -> {
                final long slashes1 = a.chars().filter(c -> c == '/').count();
                final long slashes2 = b.chars().filter(c -> c == '/').count();
                return slashes1 != slashes2 ? Long.compare(slashes1, slashes2) : a.compareTo(b);
            });
            sortedMap.putAll(unsrotedMap);

            String userUIAppsDirectory = componentListService.getUserComponentPath(req.getResourceResolver(), "ui_apps_directory_path");
            userUIAppsDirectory = userUIAppsDirectory.endsWith("/") ? userUIAppsDirectory : userUIAppsDirectory + Constants.FORWARD_SLASH;

            Utils.createFolder(userUIAppsDirectory + componentPath + Constants.FORWARD_SLASH + "_cq_dialog");
            String localurl = req.getScheme() + "://" + req.getServerName() + Constants.COLON + req.getServerPort() + componentPath + "/cq:dialog.xml";
            String responseBody = getXMLFromServer(localurl);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(new InputSource(new StringReader(responseBody)));

            Node _cqDialogNode = document.getElementsByTagName("cq:dialog").item(0);
            String title = _cqDialogNode.getAttributes().getNamedItem(com.day.cq.commons.jcr.JcrConstants.JCR_TITLE).getNodeValue();
            document.renameNode(_cqDialogNode, "", "jcr:root");
            Utils.transformDomToFile(document, userUIAppsDirectory + componentPath + "/" + "_cq_dialog");

            title = title.replaceAll(Constants.DASH_REGEX, "");
            title = title.substring(0, 1).toUpperCase() + title.substring(1) + "Model";
            String coreModelDirPath = componentListService.getUserComponentPath(req.getResourceResolver(), "core_bundle_directory_path");
            String reactModuleDirPath = componentListService.getUserComponentPath(req.getResourceResolver(), "react_module_directory_path");

            Multimap map = getPropertyMapAndTypes(document);
            LOG.debug("Multimap : " + map);

            SlingModel slingModelInfo = new SlingModel(map, "java", coreModelDirPath,  componentPath, title);
            Runnable slingModel = new WorkerThread(slingModelInfo);

            String componentPathWithoutSlash = componentPath.startsWith("/") ? StringUtils.substring(componentPath, 1) : componentPath;
            List<String> multifieldItemList = getMultiFieldItemList(document);
            String userUIAppsDirectoryWithoutSlash = userUIAppsDirectory + componentPathWithoutSlash;
            HTLModel htlModelInfo = new HTLModel(map, "html", userUIAppsDirectoryWithoutSlash, componentPath, title, multifieldItemList,coreModelDirPath);
            Runnable updateHtl = new WorkerThread(htlModelInfo);

            ReactModel reactModelInfo = new ReactModel(map,"js",reactModuleDirPath,componentPath, title, userUIAppsDirectoryWithoutSlash);
            Runnable reactModel = new WorkerThread(reactModelInfo);

            startThreads(slingModel, reactModel, updateHtl);

            JSONObject json = new JSONObject();
            json.put("result", "ok");
            resp.getWriter().write(json.toString());
        } catch (Exception e) {
            LOG.error("Error: ", e);
            resp.setContentType("text/plain");
            resp.getWriter().write("Error");
        }

    }

    /**
     *
     * @param slingModel
     * @param reactModel
     * @param updateHtl
     */
    private void startThreads(Runnable slingModel, Runnable reactModel, Runnable updateHtl) {
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        executorService.execute(slingModel);
        executorService.execute(reactModel);
        executorService.execute(updateHtl);

        executorService.shutdown();
    }

    /**
     *
     * @param document
     * @return
     */
    private List<String> getMultiFieldItemList(Document document) {
        NodeList nList = document.getElementsByTagName("field");
        List<String> list = new ArrayList<String>();

        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            Node aNode = Utils.getNode(nNode);
            NodeList parentList = aNode.getParentNode().getChildNodes();
            for (int temp1 = 0; temp1 < parentList.getLength(); temp1++) {
                Node nNode1 = parentList.item(temp1);
                if (nNode1.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode1;
                    String propName = eElement.hasAttribute("name") ? eElement.getAttribute("name") : StringUtils.EMPTY;
                    list.add(propName);
                }
            }

        }
        return list;
    }

    /**
     * @param localurl
     * @return
     * @throws IOException
     */
    private String getXMLFromServer(String localurl) throws IOException {
        HttpGet request = new HttpGet(localurl);
        String auth = Constants.DEFAULT_LOCAL_ADMIN_USER_NAME + Constants.COLON + Constants.DEFAULT_LOCAL_ADMIN_USER_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        String authHeader = "Basic " + encodedAuth;
        request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        HttpResponse response = httpClient.execute(request);

        int statusCode = response.getStatusLine().getStatusCode();
        LOG.debug("statusCode : " + statusCode);
        String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        return responseBody;
    }

    /**
     * @param document
     * @return
     * @throws XPathExpressionException
     */
    private Multimap getPropertyMapAndTypes(Document document) throws XPathExpressionException {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("//*[@name]");
        NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
        Multimap<String, String> map = LinkedHashMultimap.create();

        for (int i = 0; i < nl.getLength(); i++) {
            Node currentItem = nl.item(i);
            if (!currentItem.getParentNode().getParentNode().getParentNode().getParentNode().getNodeName().equals("field")) {
                String propName = currentItem.getAttributes().getNamedItem("name").getNodeValue();
                propName = propName.startsWith("./") ? StringUtils.substringAfter(propName, "./") : propName;
                String resourceType = currentItem.getAttributes().getNamedItem("sling:resourceType").getNodeValue();
                map.put(propName, resourceType);
            }
        }

        findMultiField(map, document);

        return map;
    }

    /**
     *
     * @param map
     * @param document
     */
    private void findMultiField(Multimap<String, String> map, Document document) {
        NodeList nList = document.getElementsByTagName("field");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String propName = eElement.getAttribute("name");
                propName = propName.startsWith("./") ? StringUtils.substringAfter(propName, "./") : propName;
                map.put(propName, Constants.RT_MULTIFIELD);
            }
        }
    }

    @Override
    protected void doGet(final SlingHttpServletRequest req,
                         final SlingHttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }


}
