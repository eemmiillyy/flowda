package flow.core.Utils;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class ClassToStringConverter {

  /**
   * Converts a Json type class with set properties to an array of strings, with 'key=value' format.
   * All properties on the class need to be set otherwise  a null exception is thrown at run time.
   * Automatically replaces all $ in a class name with dashes -
   * Automatically replaces all _ in a class name with dots .
   * @param <T>
   * @param obj
   * @return
   * @throws Exception
   */
  public <T> ArrayList<String> convertToStringArray(T obj) throws Exception {
    Field[] fields = obj.getClass().getFields();
    ArrayList<String> map = new ArrayList<String>();
    for (Field field : fields) {
      try {
        Object value = field.get(obj);
        if (value == null) {
          throw new Exception(
            "Unexpected null value found when converting Json class to string"
          );
        }
        String output =
          "'" +
          field.getName().replace("$", "-").replace("_", ".") +
          "'='" +
          value +
          "'";
        map.add(output);
      } catch (IllegalAccessException e) {
        throw new Exception(
          "Unexpected null value found when converting Json class to string"
        );
      }
    }
    return map;
  }

  /**
   *
   * Recursively converts a Json type class with set properties to a Json formatted string with \"key\":\"value\" format.
   * Only supports Json Objects with string properties and nested objects containing string properties.
   * All properties on the class need to be set otherwise  a null exception is thrown at run time.
   * Automatically replaces all $ in a class name with dashes -
   * Automatically replaces all _ in a class name with dots .
   * @param <T> Any kind of object type
   * @param obj Any kind of object
   * @param map An empty string builder instance to begin with.
   * @return Json Formatted string without the initial enclosing parenthesis, e.g. {...}
   * @throws Exception If a null value is found on the object.
   */
  public <T> String convertToJsonFormattedString(T obj, StringBuilder map)
    throws Exception {
    Field[] fields = obj.getClass().getFields();
    for (Field field : fields) {
      String fieldName = field.getName().replace("$", "-").replace("_", ".");
      try {
        Object value = field.get(obj);
        if (value instanceof String) {
          String output = "\"" + fieldName + "\":\"" + value + "\", ";
          map.append(output);
        } else {
          if (value == null) {
            throw new Exception(
              "Unexpected null value found when converting Json class to string"
            );
          }
          if (value instanceof Object) {
            map.append("\"" + fieldName + "\": {");
            convertToJsonFormattedString(value, map);
            int start = map.lastIndexOf(",");
            map.deleteCharAt(start);
            map.append("}");
          }
        }
      } catch (IllegalAccessException e) {
        throw new Exception(
          "nexpected null value found when converting Json class to string"
        );
      }
    }
    return map.toString();
  }
}
