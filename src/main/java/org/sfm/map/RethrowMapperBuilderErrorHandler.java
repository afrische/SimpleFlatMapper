package org.sfm.map;

public final class RethrowMapperBuilderErrorHandler implements
		MapperBuilderErrorHandler {

	@Override
	public void getterNotFound(final String msg) {
		throw new MapperBuildingException(msg);
	}

	@Override
	public void setterNotFound(final Class<?> target, final String property) {
		throw new MapperBuildingException("Setter for " + property + " on  " + target + " not found");
	}

}
