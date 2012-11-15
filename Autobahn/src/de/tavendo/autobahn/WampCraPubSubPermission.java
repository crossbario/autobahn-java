/******************************************************************************
 *
 *  Copyright 2012 Alejandro Hernandez
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package de.tavendo.autobahn;

public class WampCraPubSubPermission{
    
    boolean prefix;
    String uri;
    boolean pub;
    boolean sub;
    
    public WampCraPubSubPermission() {
    }
       
    public WampCraPubSubPermission(boolean prefix, String uri, boolean pub, boolean sub) {
        this.prefix = prefix;
        this.uri = uri;
        this.pub = pub;
        this.sub = sub;
    }
    
    public boolean isPrefix() {
        return prefix;
    }
    public void setPrefix(boolean prefix) {
        this.prefix = prefix;
    }
    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    public boolean isPub() {
        return pub;
    }
    public void setPub(boolean pub) {
        this.pub = pub;
    }
    public boolean isSub() {
        return sub;
    }
    public void setSub(boolean sub) {
        this.sub = sub;
    }
    
    
}   
