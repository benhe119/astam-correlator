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
//              Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////

package com.denimgroup.threadfix.cds.rest;

import com.denimgroup.threadfix.cds.rest.response.RestResponse;
import com.secdec.astam.common.data.models.Attacksurface.*;

/**
 * Created by amohammed on 6/23/2017.
 */
public interface AstamAttackSurfaceClient {

    //AttackSurface

    //AttackSurface//RawDiscoveredAttackSurface
    RestResponse<RawDiscoveredAttackSurfaceSet> getAllRawDiscoveredAttackSurfaces();
    RestResponse createRawDiscoveredAttackSurface(RawDiscoveredAttackSurface rawDiscoveredAttackSurface);

    //AttackSurface//RawDiscoveredAttackSurface/{rawDiscoveredAttackSurfaceId}
    RestResponse<RawDiscoveredAttackSurface> getRawDiscoveredAttackSurface(String rawDiscoveredAttackSurfaceId);
    RestResponse updateRawDiscoveredAttackSurface(String rawDiscoveredAttackSurfaceId,
                                                  RawDiscoveredAttackSurface  rawDiscoveredAttackSurface);
    RestResponse deleteRawDiscoveredAttackSurface(String rawDiscoveredAttackSurfaceId);

    //AtackSurface/EntryPoint

    //AtackSurface/EntryPoint/Web
    RestResponse<EntryPointWebSet> getAllEntryPointsWeb();
    RestResponse createEntryPointWeb(EntryPointWeb entryPointWeb);

    //AtackSurface/EntryPoint/Web/{entryPointWebId}
    RestResponse<EntryPointWeb> getEntryPointWeb(String entryPointWebId);
    RestResponse updateEntryPointWeb(String entryPointWebId, EntryPointWeb entryPointWeb);
    RestResponse deleteEntryPointWeb(String entryPointWebId);

    //AtackSurface/EntryPoint/Mobile
    RestResponse<EntryPointMobileSet> getAllEntryMobilePoints();
    RestResponse createEntryMobilePoint(EntryPointMobile entryPointMobile);

    //AtackSurface/EntryPoint/Mobile/{entryPointMobileId}
    RestResponse<EntryPointMobile> getEntryPointMobile(String entryPointMobileId);
    RestResponse updateEntryPointMobile(String entryPointMobileId, EntryPointMobile entryPointMobile);
    RestResponse deleteEntryPointMobile(String entryPointMobileId);
}
