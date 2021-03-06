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
//              Secure Decisions, a division of Applied Visions, Inc
//
////////////////////////////////////////////////////////////////////////

package com.denimgroup.threadfix.framework.impl.django;

import com.denimgroup.threadfix.data.entities.RouteParameter;
import com.denimgroup.threadfix.data.enums.ParameterDataType;
import com.denimgroup.threadfix.framework.util.EventBasedTokenizer;
import com.denimgroup.threadfix.framework.util.EventBasedTokenizerRunner;
import com.denimgroup.threadfix.logging.SanitizedLogger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.StreamTokenizer;
import java.util.List;

import static com.denimgroup.threadfix.CollectionUtils.list;

/**
 * Created by csotomayor on 6/21/2017.
 */
public class DjangoControllerParser implements EventBasedTokenizer {

    public static final SanitizedLogger LOG = new SanitizedLogger(DjangoControllerParser.class);
    public static final boolean logParsing = false;

    private DjangoRoute currentRoute = null;
    private List<DjangoRoute> djangoRoutes = list();
    private String url, filePath, methodName = "";
    private String workingMethodName = "";
    private boolean shouldContinue = true;

    public static List<DjangoRoute> parse(@Nonnull File file, String url, String methodName) {
        DjangoControllerParser controllerParser = new DjangoControllerParser();
        controllerParser.url = url;
        controllerParser.filePath = file.getAbsolutePath();
        controllerParser.methodName = methodName;
        EventBasedTokenizerRunner.run(file, controllerParser);

        return controllerParser.djangoRoutes;
    }

    private static final String
            METHOD_DEF = "def",
            REQUEST = "request",
            GETREQUEST = "GET",
            POSTREQUEST = "POST";

    private enum Phase {
        PARSING, IN_METHOD
    }
    private Phase           currentPhase        = Phase.PARSING;
    private MethodState     currentMethodState  = MethodState.START;

    @Override
    public boolean shouldContinue() {
        return shouldContinue;
    }

    private void log(Object string) {
        if (logParsing && string != null) {
            LOG.debug(string.toString());
        }
    }

    @Override
    public void processToken(int type, int lineNumber, String stringValue) {
        log("type  : " + type);
        log("string: " + stringValue);
        log("phase: " + currentPhase + " ");

        if (METHOD_DEF.equals(stringValue)){
            currentPhase = Phase.IN_METHOD;
            currentMethodState = MethodState.START;
            currentRoute = new DjangoRoute(url, filePath);
            currentRoute.setLineNumbers(lineNumber, 0);
            workingMethodName = null;
            djangoRoutes.add(currentRoute);
        }

        switch (currentPhase) {
            case IN_METHOD:
                processMethod(type, stringValue);
                break;
        }
    }

    private enum MethodState {
        START, PARAMS, BODY, REQUEST, PARAM
    }
    private void processMethod(int type, String stringValue) {
        log(currentMethodState);

        switch (currentMethodState) {
            case START:
                if (METHOD_DEF.equals(stringValue))
                    break;

                if (stringValue != null) {
                    if (workingMethodName == null) {
                        workingMethodName = stringValue;
                    } else {
                        workingMethodName += stringValue;
                    }
                } else if (type == '_') {
                    if (workingMethodName == null) {
                        workingMethodName = "_";
                    } else {
                        workingMethodName += '_';
                    }
                }

                if (methodName != null && methodName.equals(workingMethodName)) {
                    currentMethodState = MethodState.PARAMS;
                } else if (type == ':' || type == '(') {
                    djangoRoutes.remove(currentRoute);
                    currentPhase = Phase.PARSING;
                }

                break;
            case PARAMS:
                if (type == ')')
                    currentMethodState = MethodState.BODY;
                else if (type == StreamTokenizer.TT_WORD){
                    if (REQUEST.equals(stringValue))
                        break;
                    currentRoute.addParameter(stringValue, RouteParameter.fromDataType(stringValue, ParameterDataType.STRING));
                }
                break;
            case BODY:
                if (type == StreamTokenizer.TT_WORD && stringValue.contains(REQUEST)) {
                    if (stringValue.contains(GETREQUEST)) {
                        currentRoute.setHttpMethod(GETREQUEST);
                        currentMethodState = MethodState.PARAM;
                    } else if (stringValue.contains(POSTREQUEST)) {
                        currentRoute.setHttpMethod(POSTREQUEST);
                        currentMethodState = MethodState.PARAM;
                    }
                }
                break;
            case PARAM:
                if (type == ')') {
                    currentPhase = Phase.IN_METHOD;
                    currentMethodState = MethodState.BODY;
                } else if (stringValue != null && !stringValue.isEmpty()) {
                    currentRoute.addParameter(stringValue, RouteParameter.fromDataType(stringValue, ParameterDataType.STRING));
                    currentPhase = Phase.IN_METHOD;
                    currentMethodState = MethodState.BODY;
                }
        }
    }

}
