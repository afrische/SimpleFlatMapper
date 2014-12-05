package org.sfm.reflect.asm;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.objectweb.asm.Opcodes;
import org.sfm.jdbc.JdbcMapper;
import org.sfm.map.RowHandlerErrorHandler;
import org.sfm.map.impl.FieldMapper;
import org.sfm.reflect.*;

public class AsmFactory implements Opcodes {
	private final FactoryClassLoader factoryClassLoader;
	private final ConcurrentMap<Method, Setter<?, ?>> setterCache = new ConcurrentHashMap<Method, Setter<?, ?>>();
	private final ConcurrentMap<InstantiatorKey, Instantiator<?, ?>> instantiatorCache = new ConcurrentHashMap<InstantiatorKey, Instantiator<?, ?>>();
	
	public AsmFactory() {
		this(Thread.currentThread().getContextClassLoader());
	}
	
	public AsmFactory(ClassLoader cl) {
		factoryClassLoader = new FactoryClassLoader(cl);
	}
	
	@SuppressWarnings("unchecked")
	public <T, P> Setter<T,P> createSetter(final Method m) throws Exception {
		Setter<T,P> setter = (Setter<T, P>) setterCache.get(m);
		if (setter == null) {
			final String className = generateClassName(m);
			final byte[] bytes = generateClassByteCodes(m, className);
			final Class<?> type = factoryClassLoader.registerClass(className, bytes);
			setter = (Setter<T, P>) type.newInstance();
			setterCache.putIfAbsent(m, setter);
		}
		return setter;
	}

	private byte[] generateClassByteCodes(final Method m, final String className) throws Exception {
		final Class<?> propertyType = m.getParameterTypes()[0];
		if (AsmUtils.primitivesClassAndWrapper.contains(propertyType)) {
			return SetterBuilder.createPrimitiveSetter(className, m);
		} else {
			return SetterBuilder.createObjectSetter(className, m);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <S, T> Instantiator<S, T> createEmptyArgsInstatiantor(final Class<S> source, final Class<? extends T> target) throws Exception {
		InstantiatorKey instantiatorKey = new InstantiatorKey(target, source);
		Instantiator<S, T> instantiator = (Instantiator<S, T>) instantiatorCache.get(instantiatorKey);
		if (instantiator == null) {
			final String className = generateInstantiatorClassName(instantiatorKey);
			final byte[] bytes = ConstructorBuilder.createEmptyConstructor(className, source, target);
			final Class<?> type = factoryClassLoader.registerClass(className, bytes);
			instantiator = (Instantiator<S, T>) type.newInstance();
			instantiatorCache.putIfAbsent(instantiatorKey, instantiator);
		}
		return instantiator;
	}
	
	@SuppressWarnings("unchecked")
	public <S, T> Instantiator<S, T> createInstatiantor(final Class<?> source, final ConstructorDefinition<T> constructorDefinition,final Map<ConstructorParameter, Getter<S, ?>> injections) throws Exception {
		InstantiatorKey instantiatorKey = new InstantiatorKey(constructorDefinition, injections.keySet(), source);
		Instantiator<S, T> instantiator = (Instantiator<S, T>) instantiatorCache.get(instantiatorKey);
		if (instantiator == null) {
			final String className = generateInstantiatorClassName(instantiatorKey);
			final byte[] bytes = InstantiatorBuilder.createInstantiator(className, source, constructorDefinition, injections);
			final Class<?> type = factoryClassLoader.registerClass(className, bytes);
			
			Map<String, Getter<S, ?>> getterPerName = new HashMap<String, Getter<S, ?>>();
			for(Entry<ConstructorParameter, Getter<S, ?>> e : injections.entrySet()) {
				getterPerName.put(e.getKey().getName(), e.getValue());
			}
			
			instantiator = (Instantiator<S, T>) type.getConstructor(Map.class).newInstance(getterPerName);
			instantiatorCache.put(instantiatorKey, instantiator);
		}
		return instantiator;
	}
	
	@SuppressWarnings("unchecked")
	public <T> JdbcMapper<T> createJdbcMapper(final FieldMapper<ResultSet, T>[] mappers, final Instantiator<ResultSet, T> instantiator, final Class<T> target, RowHandlerErrorHandler errorHandler) throws Exception {
		final String className = generateClassName(mappers, ResultSet.class, target);
		final byte[] bytes = JdbcMapperAsmBuilder.dump(className, mappers, target);
		final Class<?> type = factoryClassLoader.registerClass(className, bytes);
		return (JdbcMapper<T>) type.getDeclaredConstructors()[0].newInstance(mappers, instantiator, errorHandler);
	}
	
	private final AtomicLong classNumber = new AtomicLong();
	
	private String generateInstantiatorClassName(final InstantiatorKey key) {
		StringBuilder sb = new StringBuilder();
		
		sb.append( "org.sfm.reflect.asm.")
		.append(key.getConstructor().getDeclaringClass().getPackage().getName())
		.append(".AsmInstantiator").append(key.getConstructor().getDeclaringClass().getSimpleName());
		String[] injectedParams = key.getInjectedParams();
		if (injectedParams != null) {
			for(String str : injectedParams) {
				sb.append(str.substring(0, Math.min(str.length(), 3)));
			}
		}
		sb.append(replaceArray(key.getSource().getSimpleName()));
		sb.append(Long.toHexString(classNumber.getAndIncrement()));
		return sb.toString();
	}

	private String replaceArray(String simpleName) {
		return simpleName.replace('[', 's').replace(']', '_');
	}

	private String generateClassName(final Method m) {
		return "org.sfm.reflect.asm." + m.getDeclaringClass().getPackage().getName() + 
					".AsmSetter" + m.getName()
					 + replaceArray(m.getDeclaringClass().getSimpleName())
					 + replaceArray(m.getParameterTypes()[0].getSimpleName())
					;
	}
	
	private <S, T> String generateClassName(final FieldMapper<S, T>[] mappers, final Class<S> source, final Class<T> target) {
		return "org.sfm.reflect.asm." + target.getPackage().getName() + 
					".AsmMapper" + replaceArray(source.getSimpleName()) + "2" +  replaceArray(target.getSimpleName()) + mappers.length + "_" + classNumber.getAndIncrement(); 
	}
}
