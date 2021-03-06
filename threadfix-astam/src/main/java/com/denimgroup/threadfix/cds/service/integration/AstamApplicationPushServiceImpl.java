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
package com.denimgroup.threadfix.cds.service.integration;

import com.denimgroup.threadfix.cds.messaging.AstamMessageManager;
import com.denimgroup.threadfix.cds.rest.AstamApplicationClient;
import com.denimgroup.threadfix.cds.rest.response.RestResponse;
import com.denimgroup.threadfix.cds.service.AstamApplicationPushService;
import com.denimgroup.threadfix.cds.service.UuidUpdater;
import com.denimgroup.threadfix.logging.SanitizedLogger;
import com.denimgroup.threadfix.util.ProtobufMessageUtils;
import com.secdec.astam.common.data.models.Appmgmt.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.denimgroup.threadfix.data.enums.AstamEntityType.*;
import static com.secdec.astam.common.messaging.Messaging.AstamMessage.DataMessage.DataAction.DATA_CREATE;
import static com.secdec.astam.common.messaging.Messaging.AstamMessage.DataMessage.DataAction.DATA_UPDATE;
import static com.secdec.astam.common.messaging.Messaging.AstamMessage.DataMessage.DataEntity.*;
import static com.secdec.astam.common.messaging.Messaging.AstamMessage.DataMessage.DataSetType.DATA_SET_COMPLETE;

/**
 * Created by amohammed on 6/28/2017.
 */
@Service
public class AstamApplicationPushServiceImpl implements AstamApplicationPushService {

    private static final SanitizedLogger LOGGER = new SanitizedLogger(AstamApplicationPushServiceImpl.class);


    private AstamApplicationClient applicationClient;
    private AstamMessageManager messageNotifier;
    private UuidUpdater uuidUpdater;


    @Autowired
    public AstamApplicationPushServiceImpl(AstamApplicationClient applicationClient,
                                           AstamMessageManager messageManager,
                                           UuidUpdater uuidUpdater){

        this.applicationClient = applicationClient;
        this.messageNotifier = messageManager;
        this.uuidUpdater = uuidUpdater;
    }

    @Override
    public boolean pushAppRegistration(int id, ApplicationRegistration appRegistration){
        boolean success = false;
        RestResponse<ApplicationRegistration> restResponse = applicationClient.getAppRegistration(appRegistration.getId().getValue());

        if (!restResponse.success){
            success = pushAppRegistration(id, appRegistration,  false);
        } else {
            //TODO: to allow update/PUT we need to add support for merging locally first
            LOGGER.debug("Cannot update existing entity in CDS with a stale entity.");
            success = false;
        }

        return success;
    }


    @Override
    public boolean pushAppRegistration(int id, ApplicationRegistration appRegistration, boolean doesExist){
        RestResponse<ApplicationRegistration> restResponse;
        boolean success = false;
        //TODO: remove extra steps since we are checking first with a GET request
        if (!doesExist){
            restResponse = applicationClient.createAppRegistration(appRegistration);

            if (restResponse.success){
                success = true;
                //int id = ProtobufMessageUtils.createIdFromUUID(appRegistration.getId().getValue());
                uuidUpdater.updateUUID(id, restResponse.uuid, APP_REGISTRATION);
                LOGGER.info("Application Registration successfully created in CDS. Id: " + id + " UUID: " + restResponse.uuid);

            } else if(restResponse.responseCode == 409 ){
                 //success = pushAppRegistration(id, appRegistration, true);
            }
        } else {
            restResponse = applicationClient.updateAppRegistration(appRegistration.getId().getValue(), appRegistration);

            if (restResponse.success && restResponse.responseCode == 204) {
                success = true;
                LOGGER.info("Application Registration successfully updated in CDS. UUID: " +  appRegistration.getId());
            } else {
                success = false;
            }
        }
        return success;
    }

    @Override
    public void pushAppVersionSet(ApplicationVersionSet localAppVersionSet, String appRegistrationId){
        List<ApplicationVersion> localAppVersionList = localAppVersionSet.getApplicationVersionsList();
        ApplicationVersionSet cdsAppVersionSet = null;
        List<String> entityIds = new ArrayList();
        List<ApplicationVersion> cdsAppVersionList = null;
        boolean isCreateOperation = false;

        try {
            cdsAppVersionSet = applicationClient.getAllAppVersions(appRegistrationId).getObject();
        }catch (NullPointerException e){
            LOGGER.debug("Data set does not exist in CDS. Attempting to create ...");
        }

        if(cdsAppVersionSet == null || cdsAppVersionList.isEmpty()){
            isCreateOperation = true;
        } else {
            cdsAppVersionList = cdsAppVersionSet.getApplicationVersionsList();
        }

        boolean doesExist;
        boolean success;

        for (ApplicationVersion localAppVersion: localAppVersionList){
            doesExist = false;
            success = false;

            if(!isCreateOperation){
                doesExist = cdsAppVersionList.contains(localAppVersion);
            }

            success = pushAppVersion(localAppVersion, doesExist);

            if(success) {
                entityIds.add(localAppVersion.getId().getValue());
            }
        }

        if(isCreateOperation){
            messageNotifier.notify(DATA_APPLICATION_VERSION, DATA_CREATE, DATA_SET_COMPLETE, entityIds );
        } else {
            messageNotifier.notify(DATA_APPLICATION_VERSION, DATA_UPDATE, DATA_SET_COMPLETE, entityIds );
        }
    }

    @Override
    public boolean pushAppVersion(ApplicationVersion appVersion, boolean doesExist){
        RestResponse<ApplicationVersion> restResponse;
        boolean success = false;

        if (!doesExist){
            restResponse = applicationClient.createAppVersion(appVersion);
            if (restResponse.success){
                success = true;
                int id = ProtobufMessageUtils.createIdFromUUID(appVersion.getId().getValue());
               uuidUpdater.updateUUID(id, restResponse.uuid, APP_VERSION);
                LOGGER.info("Application Version successfully created in CDS. Id: " + id + " UUID: " + restResponse.uuid);
            } else if(restResponse.responseCode == 409){
                success = pushAppVersion(appVersion, true);
            }
        } else {
            restResponse = applicationClient.updateAppVersion(appVersion.getId().getValue(), appVersion);
            if (restResponse.success) {
                success = true;
                LOGGER.info("Application Version successfully updated in CDS. UUID: " +  appVersion.getId());
            } else if(restResponse.responseCode == 422){
                success = pushAppVersion(appVersion, false);
            }
        }

        return success;
    }

    @Override
    public void pushAppEnvironmentSet(ApplicationEnvironmentSet localAppEnvironmentSet){
        List<ApplicationEnvironment> localAppEnvironmentList = localAppEnvironmentSet.getApplicationEnvironmentsList();
        ApplicationEnvironmentSet cdsAppEnvironmentSet = null;
        List<ApplicationEnvironment> cdsAppEnvironmentList = null;
        List<String> entityIds = new ArrayList<>();
        boolean isCreateOperation = false;

        try {
            cdsAppEnvironmentSet = applicationClient.getAllAppEnvironments().getObject();
        } catch (NullPointerException npe){
            LOGGER.debug("Data set does not exist in CDS. Attempting to create ...");
        }

        if(cdsAppEnvironmentSet == null || cdsAppEnvironmentSet.getApplicationEnvironmentsList().isEmpty() ){
            isCreateOperation = true;
        } else {
            cdsAppEnvironmentList = cdsAppEnvironmentSet.getApplicationEnvironmentsList();
        }

        boolean doesExist;
        boolean success;

        for (ApplicationEnvironment localAppEnvironment: localAppEnvironmentList){
            doesExist = false;
            success = false;

            if(!isCreateOperation){
                doesExist = cdsAppEnvironmentList.contains(localAppEnvironment);
            }

            success = pushAppEnvironment(localAppEnvironment, doesExist);

            if(success) {
                entityIds.add(localAppEnvironment.getId().getValue());
            }
        }

        if(isCreateOperation){
            messageNotifier.notify(DATA_APPLICATION_ENVIRONMENT, DATA_CREATE, DATA_SET_COMPLETE, entityIds );
        } else {
            messageNotifier.notify(DATA_APPLICATION_ENVIRONMENT, DATA_UPDATE, DATA_SET_COMPLETE, entityIds );
        }


    }

    @Override
    public boolean pushAppEnvironment(ApplicationEnvironment appEnvironment, boolean doesExist){
        RestResponse<ApplicationEnvironment> restResponse;
        boolean success = false;

        if (!doesExist){
            restResponse = applicationClient.createEnvironment(appEnvironment);
            if (restResponse.success){
                success = true;
                int id = ProtobufMessageUtils.createIdFromUUID(appEnvironment.getId().getValue());
                uuidUpdater.updateUUID(id, restResponse.uuid, APP_ENVIRONMENT);
                LOGGER.info("Application Environment successfully created in CDS. Id: " + id + " UUID: " + restResponse.uuid);
            } else if(restResponse.responseCode == 409){
                success = pushAppEnvironment(appEnvironment, true);
            }
        } else {
            restResponse = applicationClient.updateEnvironment(appEnvironment.getId().getValue(), appEnvironment);
            if (restResponse.success) {
                success = true;
                LOGGER.info("Application Environment successfully updated in CDS. UUID: " +  appEnvironment.getId());
            } else if(restResponse.responseCode == 422){
                success = pushAppEnvironment(appEnvironment, false);
            }
        }

        return success;
    }

    @Override
    public void pushAppDeploymentSet(ApplicationDeploymentSet localAppDeploymentSet){
        List<ApplicationDeployment> localAppDeploymentList = localAppDeploymentSet.getApplicationDeploymentsList();
        ApplicationDeploymentSet cdsAppDeploymentSet = null;
        List<ApplicationDeployment> cdsAppDeploymentsList = null;
        List<String> entityIds = new ArrayList();
        boolean isCreateOperation = false;

        try {
            cdsAppDeploymentSet = applicationClient.getAllAppDeployments().getObject();
        }catch (NullPointerException e){
            LOGGER.debug("Data set does not exist in CDS. Attempting to create ...");
        }

        if(cdsAppDeploymentSet == null || cdsAppDeploymentsList.isEmpty()) {
            isCreateOperation = true;
        } else {
            cdsAppDeploymentsList = cdsAppDeploymentSet.getApplicationDeploymentsList();
        }

        boolean doesExist;
        boolean success;

        for (ApplicationDeployment localAppDeployment: localAppDeploymentList){
            doesExist = false;
            success = false;

            if(!isCreateOperation){
                doesExist = cdsAppDeploymentsList.contains(localAppDeployment);
            }

            success = pushAppDeployment(localAppDeployment, doesExist);

            if (success){
                entityIds.add(localAppDeployment.getId().getValue());
            }
        }

        if(isCreateOperation){
            messageNotifier.notify(DATA_APPLICATION_DEPLOYMENT, DATA_CREATE, DATA_SET_COMPLETE, entityIds );
        } else {
            messageNotifier.notify(DATA_APPLICATION_DEPLOYMENT, DATA_UPDATE, DATA_SET_COMPLETE, entityIds );
        }


    }

    @Override
    public boolean pushAppDeployment(ApplicationDeployment appDeployment, boolean doesExist){
        RestResponse<ApplicationDeployment> restResponse;
        boolean success = false;

        if (!doesExist){
            restResponse = applicationClient.createAppDeployment(appDeployment);
            if (restResponse.success){
                success = true;
                int id = ProtobufMessageUtils.createIdFromUUID(appDeployment.getId().getValue());
                uuidUpdater.updateUUID(id, restResponse.uuid, APP_DEPLOYMENT);
                LOGGER.info("Application Deployment successfully created in CDS. Id: " + id + " UUID: " + restResponse.uuid);
            } else if(restResponse.responseCode == 409){
                success = pushAppDeployment(appDeployment, true);
            }
        } else {
            restResponse = applicationClient.updateAppDeployment(appDeployment.getId().getValue(), appDeployment);
            if (restResponse.success) {
                success = true;
                LOGGER.info("Application Deployment successfully updated in CDS. UUID: " +  appDeployment.getId());
            } else if(restResponse.responseCode == 422){
                success = pushAppDeployment(appDeployment, false);
            }
        }

        return success;
    }

}
