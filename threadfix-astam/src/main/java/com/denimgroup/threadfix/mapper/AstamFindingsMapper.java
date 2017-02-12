package com.denimgroup.threadfix.mapper;

import com.denimgroup.threadfix.data.entities.*;
import com.google.protobuf.Message;
import com.secdec.astam.common.data.models.Common;
import com.secdec.astam.common.data.models.Entities;
import com.secdec.astam.common.data.models.Findings;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jsemtner on 2/7/2017.
 */
public class AstamFindingsMapper {
    private List<Entities.CWE> cwes;
    private List<Entities.ExternalTool> externalTools;

    private List<Findings.DastFinding> dastFindings;
    private List<Findings.SastFinding> sastFindings;
    private List<Findings.CorrelatedFinding> correlatedFindings;

    private Map<String, Common.Severity> severityMap;

    public AstamFindingsMapper() {
        cwes = new ArrayList<Entities.CWE>();
        externalTools = new ArrayList<Entities.ExternalTool>();

        dastFindings = new ArrayList<Findings.DastFinding>();
        sastFindings = new ArrayList<Findings.SastFinding>();
        correlatedFindings = new ArrayList<Findings.CorrelatedFinding>();

        severityMap = new HashMap<String, Common.Severity>();
        severityMap.put(GenericSeverity.INFO,Common.Severity.INFO_SEVERITY);
        severityMap.put(GenericSeverity.LOW,Common.Severity.LOW_SEVERITY);
        severityMap.put(GenericSeverity.MEDIUM,Common.Severity.MEDIUM_SEVERITY);
        severityMap.put(GenericSeverity.HIGH,Common.Severity.HIGH_SEVERITY);
        severityMap.put(GenericSeverity.CRITICAL,Common.Severity.CRITICAL_SEVERITY);
    }

    private Common.UUID createUUID(String uuid) {
        return Common.UUID.newBuilder().setValue(uuid).build();
    }

    public Entities.CWE addCwe(GenericVulnerability genericVulnerability) {
        Entities.CWE cwe = Entities.CWE.newBuilder()
                .setWeaknessId(genericVulnerability.getCweId())
                .setTitle(genericVulnerability.getName()).build();

        if (!cwes.contains(cwe)) {
            cwes.add(cwe);
        }

        return cwe;
    }

    public Entities.ExternalTool addExternalTool(ChannelType channelType) {
        Entities.ExternalTool externalTool = Entities.ExternalTool.newBuilder()
                .setId(createUUID(channelType.getId().toString()))
                .setName(channelType.getName()).build();

        if (!externalTools.contains(externalTool)) {
            externalTools.add(externalTool);
        }

        return externalTool;
    }

    private Common.HttpMethod getHttpMethod(String httpMethod) {
        if (httpMethod == null)
            return null;

        return Common.HttpMethod.valueOf(httpMethod);
    }

    private Findings.DastFinding.AttackVariant getAttackVariant(Finding finding) {
        SurfaceLocation surfaceLocation = finding.getSurfaceLocation();
        String attackRequest = finding.getAttackRequest();
        String attackResponse = finding.getAttackResponse();

        Findings.DastFinding.AttackVariant.WebAttackIteration.Builder webAttackIterationBuilder =
                Findings.DastFinding.AttackVariant.WebAttackIteration.newBuilder()
                        .setResourcePath(surfaceLocation.getPath());
                        // TODO: Add .putUrlParameters(parseUrlParameters(attackRequest) or add in data model and
                        //       parse/save during scan import
                        // TODO: Add .putHeaders(parseHeaders(attackRequest)
                        // TODO: Add .setPostData(parseBody(attackRequest))

        Common.HttpMethod httpMethod = getHttpMethod(surfaceLocation.getHttpMethod());
        if (httpMethod != null) {
            webAttackIterationBuilder.setRequestMethod(httpMethod);
        }

        if (attackResponse != null) {
            webAttackIterationBuilder.setAttackResponseString(attackResponse);
        }

        Findings.DastFinding.AttackVariant.AttackStep attackStep =
                Findings.DastFinding.AttackVariant.AttackStep.newBuilder()
                        .setWebAttackStep(webAttackIterationBuilder.build()).build();

        return Findings.DastFinding.AttackVariant.newBuilder().addAttackSteps(attackStep).build();
    }

    private GenericVulnerability getGenericVulnerability(Finding finding) {
        GenericVulnerability genericVulnerability = null;

        Vulnerability vulnerability = finding.getVulnerability();
        if (vulnerability != null) {
            genericVulnerability = vulnerability.getGenericVulnerability();
        }

        return genericVulnerability;
    }

    public void addDastFindings(Scan scan) {
        List<Finding> findings = scan.getFindings();
        Entities.ExternalTool externalTool = addExternalTool(scan.getApplicationChannel().getChannelType());

        for (int i=0; i<findings.size(); i++) {
            Finding finding = findings.get(i);

            Findings.DastFinding.Builder dastFindingBuilder = Findings.DastFinding.newBuilder()
                    .setReportingExternalToolId(externalTool.getId())
                    .addAttackVariants(getAttackVariant(finding))
                    .setId(createUUID(finding.getId().toString()));

            ChannelSeverity channelSeverity = finding.getChannelSeverity();
            String toolSeverityName = channelSeverity.getName();
            if (toolSeverityName != null) {
                dastFindingBuilder.setToolDefinedSeverity(toolSeverityName);
            }

            GenericVulnerability genericVulnerability = getGenericVulnerability(finding);
            if (genericVulnerability != null) {
                Entities.CWE cwe = addCwe(genericVulnerability);
                dastFindingBuilder.setName(genericVulnerability.getName())
                        .addCweIds(cwe.getWeaknessId());
            }

            dastFindings.add(dastFindingBuilder.build());
        }
    }

    private List<Findings.SastFinding.TraceNode> getTraceNodes(Finding finding) {
        List<DataFlowElement> dataFlowElements = finding.getDataFlowElements();
        List<Findings.SastFinding.TraceNode> traceNodeList = new ArrayList<Findings.SastFinding.TraceNode>();
        for (int i=0; i<dataFlowElements.size(); i++) {
            DataFlowElement dataFlowElement = dataFlowElements.get(i);
            Findings.SastFinding.TraceNode traceNode = Findings.SastFinding.TraceNode.newBuilder()
                    .setColumn(dataFlowElement.getColumnNumber())
                    .setLine(dataFlowElement.getLineNumber())
                    .setFile(dataFlowElement.getSourceFileName())
                    .setLineOfCode(dataFlowElement.getLineText()).build();

            traceNodeList.add(traceNode);
        }

        return traceNodeList;
    }

    public void addSastFindings(Scan scan) {
        List<Finding> findings = scan.getFindings();
        Entities.ExternalTool externalTool = addExternalTool(scan.getApplicationChannel().getChannelType());

        for (int i=0; i<findings.size(); i++) {
            Finding finding = findings.get(i);

            Findings.SastFinding.Builder sastFindingBuilder = Findings.SastFinding.newBuilder()
                    .setReportingExternalToolId(externalTool.getId())
                    .setToolDefinedSeverity(finding.getChannelSeverity().getName())
                    .addAllTrace(getTraceNodes(finding))
                    .setId(createUUID(finding.getId().toString()));

            GenericVulnerability genericVulnerability = getGenericVulnerability(finding);
            if (genericVulnerability != null) {
                Entities.CWE cwe = addCwe(genericVulnerability);
                sastFindingBuilder.setName(genericVulnerability.getName())
                        .addCweIds(cwe.getWeaknessId());
            }

            String description = finding.getLongDescription();
            if (!StringUtils.isEmpty(description)) {
                sastFindingBuilder.setDescription(description);
            }

            sastFindings.add(sastFindingBuilder.build());
        }
    }

    private List<Common.UUID> getUuidsForFindings(List<Finding> findingList) {
        List<Common.UUID> uuids = new ArrayList<Common.UUID>();

        for (int i=0; i<findingList.size(); i++) {
            Finding finding = findingList.get(i);
            uuids.add(createUUID(finding.getId().toString()));
        }

        return uuids;
    }

    public void addCorrelatedFindings(List<Vulnerability> vulnerabilityList) {
        for (int i=0; i<vulnerabilityList.size(); i++) {
            Vulnerability vulnerability = vulnerabilityList.get(i);
            List<Finding> findings = vulnerability.getFindings();
            GenericSeverity severity = vulnerability.getGenericSeverity();

            if (findings.size() < 2) {
                continue;
            }

            Findings.CorrelatedFinding.Builder correlatedFindingBuilder = Findings.CorrelatedFinding.newBuilder()
                    .addAllDastFindingIds(getUuidsForFindings(vulnerability.getDynamicFindings()))
                    .addAllSastFindingIds(getUuidsForFindings(vulnerability.getStaticFindings()))
                    .setSeverity(severityMap.get(severity.getName()));

            GenericVulnerability genericVulnerability = vulnerability.getGenericVulnerability();
            if (genericVulnerability != null) {
                correlatedFindingBuilder.setName(genericVulnerability.getName())
                        .addApplicableCwes(genericVulnerability.getCweId());
            }

            correlatedFindings.add(correlatedFindingBuilder.build());
        }
    }

    private static <T extends Message> void writeListToOutput(List<T> messageList, OutputStream output)
            throws IOException {
        for (int i=0; i<messageList.size(); i++) {
            messageList.get(i).writeTo(output);
        }
    }

    public void writeFindingsToOutput(OutputStream outputStream) throws IOException {
        writeListToOutput(cwes, outputStream);
        writeListToOutput(externalTools, outputStream);
        writeListToOutput(dastFindings, outputStream);
        writeListToOutput(sastFindings, outputStream);
        writeListToOutput(correlatedFindings, outputStream);


        // TODO: add cweset, externaltoolset
        // TODO: add rawfindingsset, correlationresultset
    }
}
