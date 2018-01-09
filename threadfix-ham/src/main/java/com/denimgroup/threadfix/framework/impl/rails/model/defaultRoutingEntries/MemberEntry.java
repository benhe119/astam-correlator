////////////////////////////////////////////////////////////////////////
//
//     Copyright (C) 2017 Applied Visions - http://securedecisions.com
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     This material is based on research sponsored by the Department of Homeland
//     Security (DHS) Science and Technology Directorate, Cyber Security Division
//     (DHS S&T/CSD) via contract number HHSP233201600058C.
//
//     Contributor(s):
//              Secure Decisions, a division of Applied Visions, Inc
//
////////////////////////////////////////////////////////////////////////

package com.denimgroup.threadfix.framework.impl.rails.model.defaultRoutingEntries;

// http://guides.rubyonrails.org/routing.html#adding-member-routes
// https://stackoverflow.com/questions/3028653/difference-between-collection-route-and-member-route-in-ruby-on-rails


import com.denimgroup.threadfix.framework.impl.rails.model.AbstractRailsRoutingEntry;
import com.denimgroup.threadfix.framework.impl.rails.model.PathHttpMethod;
import com.denimgroup.threadfix.framework.impl.rails.model.RailsRoutingEntry;
import com.denimgroup.threadfix.framework.impl.rails.model.RouteParameterValueType;
import com.denimgroup.threadfix.framework.util.PathUtil;

import javax.annotation.Nonnull;
import java.util.Collection;

//  Member routes are added within a 'resource' or 'resources' scope to make
//  new routes relative to the exposed instances for each route.
public class MemberEntry extends AbstractRailsRoutingEntry {

    String endpoint = null;

    @Override
    public String getPrimaryPath() {
        String basePath = getParent().getPrimaryPath();
        basePath = PathUtil.combine(basePath, ":id");
        return PathUtil.combine(basePath, endpoint);
    }

    @Override
    public Collection<PathHttpMethod> getPaths() {
        return null;
    }

    @Override
    public String getControllerName() {
        return getParentController();
    }

    @Override
    public String getModule() {
        return getParentModule();
    }

    @Override
    public void onParameter(String name, String value, RouteParameterValueType parameterType) {
        super.onParameter(name, value, parameterType);
    }

    @Nonnull
    @Override
    public RailsRoutingEntry cloneEntry() {
        MemberEntry clone = new MemberEntry();
        clone.endpoint = endpoint;
        cloneChildrenInto(clone);
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("member (");
        result.append(getChildren().size());
        result.append(" subroutes)");
        return result.toString();
    }
}
