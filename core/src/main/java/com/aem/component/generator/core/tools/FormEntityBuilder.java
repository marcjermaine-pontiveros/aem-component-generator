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
package com.aem.component.generator.core.tools;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FormEntityBuilder
{
    public static final String DEFAULT_ENCODING = "UTF-8";
    private final List<NameValuePair> params;
    private String encoding;
    public static FormEntityBuilder create() { return new FormEntityBuilder(); }
    FormEntityBuilder() {
        this.params = new ArrayList();
        this.encoding = "UTF-8";
    }

    public FormEntityBuilder addAllParameters(Map<String, String> parameters) {
        if (parameters != null) {
            for (String key : parameters.keySet()) {
                addParameter(key, (String)parameters.get(key));
            }
        }
        return this;
    }
    public FormEntityBuilder addAllParameters(List<NameValuePair> parameters) {
        if (parameters != null) {
            this.params.addAll(parameters);
        }
        return this;
    }

    public FormEntityBuilder addParameter(String name, String value) {
        this.params.add(new BasicNameValuePair(name, value));
        return this;
    }

    public FormEntityBuilder setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    public UrlEncodedFormEntity build() {
        try {
            return new UrlEncodedFormEntity(this.params, this.encoding);
        } catch (UnsupportedEncodingException ue) {
            throw new Error("Unexpected UnsupportedEncodingException", ue);
        }
    }
}
