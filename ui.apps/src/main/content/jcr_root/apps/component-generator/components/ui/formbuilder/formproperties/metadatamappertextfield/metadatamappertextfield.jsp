<%--
  Copyright 2020 Sumanta Pakira

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

--%>
<%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@ page session="false" contentType="text/html" pageEncoding="utf-8"
           import="com.adobe.granite.ui.components.formbuilder.FormResourceManager,
                 org.apache.jackrabbit.util.Text,
         		 org.apache.sling.api.resource.Resource,
         		 org.apache.sling.api.resource.ValueMap,
                 com.adobe.granite.ui.components.Config,
                 com.adobe.granite.ui.components.AttrBuilder,
                 org.apache.commons.lang.StringUtils,
         		 java.util.HashMap" %><%
    ValueMap fieldProperties = resource.adaptTo(ValueMap.class);

    Config cfg = null;

    String fieldRelativeResourcePath = (String)request.getAttribute("cq.dam.metadataschema.builder.field.relativeresource");
    if (StringUtils.isNotBlank(fieldRelativeResourcePath)) {
        Resource fieldRelativeResource = resource.getChild(fieldRelativeResourcePath);
        if (fieldRelativeResource != null) {
            cfg = new Config(fieldRelativeResource);
        }
    }
    if (cfg == null) {
        cfg = new Config(resource);
    }

    String rootPath = "/";
    HashMap<String, Object> values = new HashMap<String, Object>();
    values.put("granite:class",        "field-mvtext-descriptor property-picker-field22 propmap-"+resource.getName() );
    values.put("fieldLabel",   i18n.get("Map to property"));
//  values.put("emptyText",    i18n.get("enter metadata key eg ./jcr:content/metadata/dc:title"));
// values.put("rootPath",    rootPath);
//   values.put("value",        cfg.get("name", "./jcr:content/metadata/default"));
// values.put("name", "./items/" + resource.getName() + (fieldRelativeResourcePath != null ? "/"+fieldRelativeResourcePath: "") +"/name");
// values.put("deleteHint", false);
// values.put("pickerSrc", "/mnt/overlay/dam/gui/content/commons/propertypicker/picker.html?_charset_=utf-8&root=" + Text.escape(rootPath));
// values.put("suggestionSrc", "/mnt/overlay/dam/gui/content/commons/propertypicker/suggestion.html?_charset_=utf-8&root=" + Text.escape(rootPath) + "{&query}");

    FormResourceManager formResourceManager = sling.getService(FormResourceManager.class);
    Resource labelFieldResource = formResourceManager.getDefaultPropertyFieldResource(resource, values);
    
    // initialize hidden input to save MSM inheritance property lock attribute
/*String propLockInputName = "./items/" + resource.getName() + (fieldRelativeResourcePath != null ? "/" + fieldRelativeResourcePath : "") + "/granite:data/cq-msm-lockable";
    String propLockInputValue = cfg.get("name", "./jcr:content/metadata/default");
    propLockInputValue = propLockInputValue.replaceAll("/jcr:content", "");
    AttrBuilder propLockInputAttrs = new AttrBuilder(request, xssAPI);
    propLockInputAttrs.addClass("msm-property-lock-field");
    propLockInputAttrs.add("type", "hidden");
    propLockInputAttrs.add("name", propLockInputName); 
    propLockInputAttrs.add("value", propLockInputValue);
    if (cfg.get("disabled", false) || !isValidLockablePath(propLockInputValue)) {
        propLockInputAttrs.add("disabled", "disabled");
    }*/

%><sling:include resource="<%= labelFieldResource %>" resourceType="granite/ui/components/coral/foundation/form/textfield"/>



<%!
    /**
     * Validate configured property path for MSM lockable attribute
     * Valid paths: ./myProperty || ./metadata/myProperty
     * 
     * @param path String to validate
     * @return True if path is valid
     */
    private boolean isValidLockablePath(String path) {
        boolean isValid = true;
        path = StringUtils.stripStart(path, "./");
        String[] segments = path.split("/");
        if (segments.length == 0 ||
            segments.length > 2 || 
            (segments.length == 1 && "metadata".equalsIgnoreCase(segments[0])) ||
            (segments.length == 2 && !"metadata".equalsIgnoreCase(segments[0]))) {
            isValid = false;
        }
        return isValid;
    }
%>