////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2015 Denim Group, Ltd.
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
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s): Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////
package com.denimgroup.threadfix.data.entities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ModelField {

	@Nonnull
	private final String fieldType, parameterKey;
	private boolean isOptional;

	public ModelField(@Nonnull String fieldType, @Nonnull String methodName) {
		this(fieldType, methodName, false);
	}

	public ModelField(@Nonnull String fieldType, @Nonnull String methodName, boolean isOptional) {
		this.fieldType = fieldType;
		this.parameterKey = getParameterKey(methodName);
		this.isOptional = isOptional;
	}

	@Nonnull
	private String getParameterKey(@Nonnull String methodCall) {
		String propertyName = methodCall;

		if (methodCall.startsWith("get") && methodCall.length() > 3) {
			// transform from bean accessor to parameter name
			propertyName = methodCall.substring(3);
			propertyName = propertyName.substring(0,1).toLowerCase() + propertyName.substring(1);
		}

		return propertyName;
	}

	@Nonnull
	public String getType() {
		return fieldType;
	}

	@Nonnull
	public String getParameterKey() {
		return parameterKey;
	}

	public boolean isPrimitiveType() {
		return "Integer".equals(fieldType) || "String".equals(fieldType) || "int".equals(fieldType);
	}

	public boolean isOptional() {
		return isOptional;
	}

	@Nonnull
	@Override
	public String toString() {
		return parameterKey + ":" + fieldType + (isOptional ? "?" : "");
	}

	@Override
	public int hashCode() {
		return fieldType.hashCode() * 37 + parameterKey.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof ModelField && object.hashCode() == hashCode();
	}
}

