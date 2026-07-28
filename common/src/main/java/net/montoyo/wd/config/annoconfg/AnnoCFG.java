package net.montoyo.wd.config.annoconfg;

import net.montoyo.wd.config.annoconfg.annotation.format.*;
import net.montoyo.wd.config.annoconfg.annotation.value.Default;
import net.montoyo.wd.config.annoconfg.handle.UnsafeHandle;
import net.montoyo.wd.config.annoconfg.util.EnumType;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

public class AnnoCFG {
	private final HashMap<String, ConfigEntry> handles = new HashMap<>();
	
	private static final ArrayList<AnnoCFG> configs = new ArrayList<>();
	private final Method postInit;
	
	public AnnoCFG(Object bus, Class<?> clazz) {
		// Stub - no config system for now
		setup("", clazz);
		configs.add(this);
		
		Method m = null;
		try {
			m = clazz.getDeclaredMethod("postLoad");
		} catch (Throwable ignored) {
		}
		postInit = m;
		
		Config configDescriptor = clazz.getAnnotation(Config.class);
		if (configDescriptor != null) {
			// Stub - no file creation for now
		}
	}
	
	protected void setupCommentsAndTranslations(AnnotatedElement element, Object builder, String... additionalLines) {
		// Stub implementation
	}
	
	public void setup(String dir, Class<?> clazz) {
		if (dir.startsWith(".")) dir = dir.substring(1);
		
		for (Field field : clazz.getFields()) {
			if (field.canAccess(null)) {
				Skip skip = field.getAnnotation(Skip.class);
				if (skip != null) continue;
				
				Name name = field.getAnnotation(Name.class);
				
				String nameStr = field.getName();
				if (name != null) nameStr = name.value();
				
				Supplier<?> value;
				
				Default defaultValue = field.getAnnotation(Default.class);
				try {
					switch (EnumType.forClass(field.getType())) {
						case INT -> {
							value = () -> defaultValue.valueI();
						}
						case LONG -> {
							value = () -> defaultValue.valueL();
						}
						case DOUBLE -> {
							value = () -> defaultValue.valueD();
						}
						case BOOLEAN -> {
							value = () -> defaultValue.valueBoolean();
						}
						case OTHER -> {
							Class<?> fieldType = field.getType();
							if (fieldType.equals(String[].class)) {
								value = () -> new String[]{defaultValue.valueStr()};
							} else if (fieldType.equals(String.class)) {
								value = () -> defaultValue.valueStr();
							} else
								throw new RuntimeException("NYI " + field.getType());
						}
						default -> throw new RuntimeException("NYI " + field.getType());
					}
				} catch (NullPointerException npe) {
					String inf = "";
					if (npe.getMessage().contains("\"value.Default\""))
						inf = " this is likely due to a missing default.";
					throw new RuntimeException("A null pointer occurred on " + field.getName() + inf, npe);
				}
				
				Object o;
				try {
					o = field.get(null);
				} catch (Throwable ignored) {
				}
				UnsafeHandle handle = new UnsafeHandle(field);
				o = handle.get();
				handle.set(o);
				
				handles.put(dir + "." + nameStr, new ConfigEntry(
						handle, value::get
				));
			}
		}
		
		for (Class<?> nestMember : clazz.getClasses()) {
			if (nestMember == clazz) continue;
			if (!nestMember.getName().startsWith(clazz.getName())) continue;
			CFGSegment segment = nestMember.getAnnotation(CFGSegment.class);
			if (segment == null) {
				throw new RuntimeException("NYI: default name for " + nestMember);
			}
			String name = segment.value();
			
			setup(dir + "." + name, nestMember);
		}
	}
	
	public void onConfigChange(Object event) {
		for (String s : handles.keySet()) {
			ConfigEntry entry = handles.get(s);
			entry.handle.set(entry.supplier.get());
		}
		try {
			postInit.invoke(null);
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}
}
