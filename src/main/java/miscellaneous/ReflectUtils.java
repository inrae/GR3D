/*
 * Copyright (C) 2012 dumoulin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package miscellaneous;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.ArrayUtils;

/**
 *
 * @author dumoulin
 */
public class ReflectUtils {

    private static final Logger LOGGER = Logger.getLogger(ReflectUtils.class.getName());

    /**
     *
     * @param type
     * @param name
     * @return null if the field wasn't found
     */
    public static Field getField(Class type, String name) throws NoSuchFieldException {
        Class current = type;
        while (current != null) {
            for (Field f : current.getDeclaredFields()) {
                if (f.getName().equals(name)) {
                    return f;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchFieldException("The field " + name + " hasn't been found in the class " + type.getName());
    }

    public static Fetcher getFetcher(Class type, String name) throws NoSuchFieldException {
        Class current = type;
        while (current != null) {
            for (Field f : current.getDeclaredFields()) {
                if (f.getName().equals(name)) {
                    return new FieldFetcher(f);
                }
            }
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(name)) {
                    return new MethodFetcher(m);
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchFieldException("The field or method " + name + " hasn't been found in the class " + type.getName());
    }

    /**
     *
     * @param field
     * @param instance
     * @return null if a problem has occured during the introspection.
     */
    public static Object getFieldValue(Field field, Object instance) {
        boolean old = field.isAccessible();
        field.setAccessible(true);
        Object value = null;
        try {
            value = field.get(instance);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during field value retrieving", ex);
        }
        field.setAccessible(old);
        return value;
    }

    /**
     *
     * @param fieldname
     * @param instance
     * @return null if a problem has occured during the introspection.
     */
    public static Object getFieldValue(String fieldname, Object instance) throws NoSuchFieldException {
        Object value = null;
        Field field = getField(instance.getClass(), fieldname);
        if (field != null) {
            value = getFieldValue(field, instance);
        }
        return value;
    }

    /**
     *
     * @param methodName
     * @param instance
     * @param parameters
     * @return null if a problem has occured during the introspection.
     */
    public static Object invokeMethod(String methodName, Object instance, Object... parameters) {
        Object result = null;
        Class[] parametersType = new Class[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parametersType[i] = parameters[i].getClass();
        }
        try {
            Method method = instance.getClass().getDeclaredMethod(methodName, parametersType);
            boolean accessible = method.isAccessible();
            method.setAccessible(true);
            result = method.invoke(instance, parameters);
            method.setAccessible(accessible);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during method invocation " + instance.getClass() + "." + methodName + "(" + ArrayUtils.toString(parameters) + ")", ex);
        }
        return result;
    }

    public static void setField(Field field, Object instance, Object value) throws IllegalAccessException, IllegalArgumentException {
        boolean old = field.isAccessible();
        field.setAccessible(true);
        field.set(instance, value);
        field.setAccessible(old);

    }

    public static void setField(String fieldname, Object instance, Object value) throws IllegalAccessException, IllegalArgumentException, NoSuchFieldException {
        Field field = getField(instance.getClass(), fieldname);
        if (field != null) {
            setField(field, instance, value);
        }
    }

    /**
     *
     * @param instance
     * @param path The path of the field to retrieve from the class of the
     * instance given. The path must contain a dot "." for accessing nested
     * fields. The element of a list can be accessed by its index. For example :
     * <tt>elements.2.aField</tt>
     * @return
     * @throws NoSuchFieldException
     */
    public static FieldOnInstance getFieldOnInstance(Object instance, String path) throws NoSuchFieldException {
        if (path.contains(".")) {
            String[] nodes = path.split("\\.");
            for (int i = 0; i < nodes.length - 1; i++) {
                if (nodes[i].matches("[0-9]*")) {
                    instance = ((List) instance).get(Integer.parseInt(nodes[i]));
                } else {
                    instance = ReflectUtils.getFieldValue(nodes[i], instance);
                }
            }
            path = nodes[nodes.length - 1];
        }
        return new FieldOnInstance(instance, getField(instance.getClass(), path));
    }

    public static FetcherOnInstance getFetcherOnInstance(Object instance, String path) throws NoSuchFieldException {
        if (path.contains(".")) {
            String[] nodes = path.split("\\.");
            for (int i = 0; i < nodes.length - 1; i++) {
                if (nodes[i].matches("[0-9]*")) {
                    instance = ((List) instance).get(Integer.parseInt(nodes[i]));
                } else {
                    instance = ReflectUtils.getFieldValue(nodes[i], instance);
                }
            }
            path = nodes[nodes.length - 1];
        }
        return new FetcherOnInstance(instance, getFetcher(instance.getClass(), path));
    }

    public static void setFieldValueFromPath(Object instance, String path, Object value) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        getFieldOnInstance(instance, path).setValue(value);
    }

    public static Object getValueFromPath(Object instance, String path) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        return getFetcherOnInstance(instance, path).getValue();
    }

    public static abstract class Fetcher {

        public abstract Object getValue(Object instance) throws IllegalArgumentException, IllegalAccessException,
                InvocationTargetException;
    }

    public static class MethodFetcher extends Fetcher {

        private Method method;

        public MethodFetcher(Method method) {
            this.method = method;
        }

        @Override
        public Object getValue(Object instance) throws IllegalArgumentException, IllegalAccessException,
                InvocationTargetException {
            return method.invoke(instance, new Object[]{});
        }
    }

    public static class FieldFetcher extends Fetcher {

        private Field field;

        public FieldFetcher(Field field) {
            this.field = field;
        }

        @Override
        public Object getValue(Object instance) throws IllegalArgumentException, IllegalAccessException,
                InvocationTargetException {
            return field.get(instance);
        }
    }

    public static class FetcherOnInstance {

        private final Object instance;
        private final Fetcher fetcher;

        public FetcherOnInstance(Object instance, Fetcher fetcher) {
            this.instance = instance;
            this.fetcher = fetcher;
        }

        public Object getInstance() {
            return instance;
        }

        public Object getValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            return fetcher.getValue(instance);
        }
    }

    /**
     * Class that wraps a field with an instance on which will be
     * retrieved/update a field.
     */
    public static class FieldOnInstance {

        private final Object instance;
        private final Field field;

        public FieldOnInstance(Object instance, Field field) {
            this.instance = instance;
            this.field = field;
        }

        public Field getField() {
            return field;
        }

        public Object getInstance() {
            return instance;
        }

        public void setValue(Object value) throws IllegalAccessException {
            setField(field, instance, value);
        }

        public Object getValue() throws IllegalAccessException {
            return getFieldValue(field, instance);
        }
    }
}
