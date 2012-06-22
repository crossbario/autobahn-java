/******************************************************************************
 *
 *  Copyright 2011-2012 Tavendo GmbH
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

import java.util.HashMap;

/**
 * Mapping between CURIEs and URIs.
 * Provides a two-way mapping between CURIEs (Compact URI Expressions) and
 * full URIs.
 *
 * \see http://www.w3.org/TR/curie/
 *
 * \todo Prefixes MUST be NCNames (http://www.w3.org/TR/1999/REC-xml-names-19990114/#NT-NCName)
 *
 * \todo Work in the details of http://www.w3.org/TR/curie/ (default prefixes, ..)
 */
public class PrefixMap {

   private final HashMap<String, String> mPrefixes = new HashMap<String, String>();
   private final HashMap<String, String> mUris = new HashMap<String, String>();

   /**
    * Set mapping of prefix to URI.
    *
    * @param prefix     Prefix to be mapped.
    * @param uri        URI the prefix is to be mapped to.
    */
   public void set(String prefix, String uri) {
      mPrefixes.put(prefix, uri);
      mUris.put(uri, prefix);
   }

   /**
    * Returns the URI for the prefix or None if prefix has no mapped URI.
    *
    * @param prefix     Prefix to look up.
    * @return           Mapped URI for prefix or None.
    */
   public String get(String prefix) {
      return mPrefixes.get(prefix);
   }

   /**
    * Remove mapping of prefix to URI.
    *
    * @param prefix     Prefix for which mapping should be removed.
    * @return           The URI the prefix was mapped to (when removed),
    *                   or null when prefix is unmapped (so there wasn't
    *                   anything to remove).
    */
   public String remove(String prefix) {
      if (mPrefixes.containsKey(prefix)) {
         String uri = mPrefixes.get(prefix);
         mPrefixes.remove(prefix);
         mUris.remove(uri);
         return uri;
      } else {
         return null;
      }
   }

   /**
    * Remove all prefix mappings.
    */
   public void clear() {
      mPrefixes.clear();
      mUris.clear();
   }

   /**
    * Resolve given CURIE to full URI.
    *
    * @param curie         CURIE (i.e. "rdf:label").
    * @return              Full URI for CURIE or None.
    */
   public String resolve(String curie) {
      int i = curie.indexOf(':');
      if (i > 0) {
         String prefix = curie.substring(0, i);
         if (mPrefixes.containsKey(prefix)) {
            return mPrefixes.get(prefix) + curie.substring(i + 1);
         }
      }
      return null;
   }

   /**
    * Resolve given CURIE/URI and return string verbatim if cannot be resolved.
    *
    * @param curieOrUri    CURIE or URI.
    * @return              Full URI for CURIE or original string.
    */
   public String resolveOrPass(String curieOrUri) {

      String u = resolve(curieOrUri);
      if (u != null) {
         return u;
      } else {
         return curieOrUri;
      }
   }

   /**
    * Shrink given URI to CURIE. If no appropriate prefix mapping is available,
    * return original URI.
    *
    * @param uri     URI to shrink.
    * @return        CURIE or original URI.
    */
   public String shrink(String uri) {

      for (int i = uri.length(); i > 0; --i) {
         String u = uri.substring(0, i);
         String p = mUris.get(u);
         if (p != null) {
            return p + ':' + uri.substring(i);
         }
      }
      return uri;
   }

}
