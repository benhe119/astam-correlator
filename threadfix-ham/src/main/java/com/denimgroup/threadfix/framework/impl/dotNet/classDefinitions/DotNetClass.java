package com.denimgroup.threadfix.framework.impl.dotNet.classDefinitions;

import java.util.List;

import static com.denimgroup.threadfix.CollectionUtils.list;

public class DotNetClass {
    private String name;
    private String namespace;
    private List<String> templateParameterNames = list();
    private List<String> baseTypes = list();
    private boolean isStatic = false;

    private String filePath;

    private List<DotNetMethod> methods = list();
    private List<DotNetAttribute> attributes = list();

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<String> getTemplateParameterNames() {
        return templateParameterNames;
    }

    public void addTemplateParameterName(String templateParamName) {
        templateParameterNames.add(templateParamName);
    }

    public List<String> getBaseTypes() {
        return baseTypes;
    }

    public void addBaseType(String baseType) {
        baseTypes.add(baseType);
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setIsStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public List<DotNetMethod> getMethods() {
        return methods;
    }

    public void addMethod(DotNetMethod method) {
        methods.add(method);
    }

    public List<DotNetAttribute> getAttributes() {
        return attributes;
    }

    public void addAttribute(DotNetAttribute attribute) {
        attributes.add(attribute);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isStatic) {
            sb.append("static ");
        }
        sb.append("class ");

        if (name == null) {
            sb.append("{NullName}");
        } else if (name.isEmpty()) {
            sb.append("{Unnammed}");
        } else {
            sb.append(name);
        }

        if (!templateParameterNames.isEmpty()) {
            sb.append('<');
            boolean isFirst = true;
            for (String paramName : templateParameterNames) {
                if (!isFirst) {
                    sb.append(", ");
                }
                if (paramName == null) {
                    sb.append("{NullName}");
                } else if (paramName.isEmpty()) {
                    sb.append("{EmptyName}");
                } else {
                    sb.append(paramName);
                }
                isFirst = false;
            }
            sb.append('>');
        }

        if (!baseTypes.isEmpty()) {
            sb.append(" : ");
            boolean isFirst = true;
            for (String baseType : baseTypes) {
                if (!isFirst) {
                    sb.append(", ");
                }
                if (baseType == null) {
                    sb.append("{NullBaseType}");
                } else if (baseType.isEmpty()) {
                    sb.append("{EmptyBaseType}");
                } else {
                    sb.append(baseType);
                }
                isFirst = false;
            }
        }

        sb.append(" (");
        sb.append(methods.size());
        sb.append(" methods, ");
        sb.append(attributes.size());
        sb.append(" attributes)");
        return sb.toString();
    }
}
