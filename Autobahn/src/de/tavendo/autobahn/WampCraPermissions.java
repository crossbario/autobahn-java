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

public class WampCraPermissions {
    
    public WampCraRpcPermission[] rpc;
    public WampCraPubSubPermission[] pubsub;
    
    public WampCraPermissions() {
    }

    public WampCraPermissions(WampCraRpcPermission[] rpc, WampCraPubSubPermission[] pubsub) {
        this.rpc = rpc;
        this.pubsub = pubsub;
    }

    public WampCraRpcPermission[] getRpc() {
        return rpc;
    }

    public void setRpc(WampCraRpcPermission[] rpc) {
        this.rpc = rpc;
    }

    public WampCraPubSubPermission[] getPubsub() {
        return pubsub;
    }

    public void setPubsub(WampCraPubSubPermission[] pubsub) {
        this.pubsub = pubsub;
    }
    
    
    
}
