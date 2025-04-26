package io.github.victormadu.display;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.victormadu.display.annotation.Column;


public class Display {
   
    public void table(Object o) {
        if (o != null) {
            table(o, o.getClass());
        } 
    }

    public void table(Object o, Class<?> itemType) {
        System.out.println(tableStringOf(listOf(o), itemType));
    }


    private List<?> listOf(Object o) {
        if (o == null) {
            return Collections.emptyList();
        } else if (o instanceof List) {
            return (List<?>) o;
        } else if (o instanceof Iterable) {
            List<Object> list = new ArrayList<>();
            ((Iterable<?>) o).forEach(list::add);
            return list;
        } else if (o instanceof Object[]) {
            return Arrays.asList((Object[]) o);
        } else if (o.getClass().isArray()) {
            int length = Array.getLength(o);
            List<Object> list = new ArrayList<>(length);
            
            for (int i = 0; i < length; i++) {
                list.add(Array.get(o, i));
            }
            return list;
        } else {
            return Collections.singletonList(o);
        }
    }

  
    private String tableStringOf(List<?> rows, Class<?> itemType) {

        if (Throwable.class.isAssignableFrom(itemType)) {
            if (rows.isEmpty()) return "";
            if (rows.size() == 1) {
                return ((Throwable) rows.get(0)).getMessage();
            }
            return rows.toString();
        }

        List<Field> fields = columnFields(itemType);
        List<String> headers = headers(fields);
        List<List<String>> rowsData = contents(rows, fields);


        if (fields.isEmpty()) {
            if (rowsData.isEmpty()) {
                return "";
            }

            headers = rowsData.get(0).stream().map((r) -> "").collect(java.util.stream.Collectors.toList());
        }


        StringBuilder sb = new StringBuilder();        
        int[] colWidths = new int[headers.size()];

        for (int i = 0; i < headers.size(); i++) {
            colWidths[i] = headers.get(i).length();
        }
        for (List<String> row : rowsData) {
            for (int i = 0; i < row.size(); i++) {
                colWidths[i] = Math.max(colWidths[i], row.get(i).length());
            }
        }

        String horizontal = buildSeparator(colWidths, '+', '-', '+');
        
        sb.append(horizontal).append("\n");
        sb.append(buildRow(headers, colWidths)).append("\n");
        sb.append(horizontal).append("\n"); 

        for (List<String> row : rowsData) {
            sb.append(buildRow(row, colWidths)).append("\n");
            sb.append(horizontal).append("\n"); 
        }

        return sb.substring(0, sb.length() - 2) + "+";
    }
    
    private List<List<String>> contents(List<?> rows, List<Field> fields) {
        if (fields.isEmpty()) {
            return rows.stream()
                .map(r -> Collections.singletonList(r!= null ? r.toString() : ""))
                .collect(java.util.stream.Collectors.toList());
        } else {
            return rows.stream()
               .map(r -> {
                   List<String> row = new ArrayList<>();
                   
                   for (Field field : fields) {
                       field.setAccessible(true);
                       try {
                           Object value = field.get(r);
                           if (value instanceof Optional) {
                               value = ((Optional<?>) value).orElse(null);
                           }   
                           row.add(value!= null? value.toString() : "");
                       } catch (IllegalAccessException e) {
                           row.add("");
                       }
                   }
                   return row;
               }).collect(java.util.stream.Collectors.toList());
        }
    }

    private String buildRow(List<String> columns, int[] widths) {
        StringBuilder row = new StringBuilder("|");
      
        for (int i = 0; i < columns.size(); i++) {
            String cell = pad(columns.get(i), widths[i]);
            row.append(" ").append(cell).append(" |");
        }
      
        return row.toString();
    }

    private String buildSeparator(int[] widths, char left, char fill, char right) {
        StringBuilder sep = new StringBuilder();
       
        sep.append(left);
       
        for (int width : widths) {
            sep.append(fill);
            for (int i = 0; i < width; i++) {
                sep.append(fill);
            }
            sep.append(fill).append(right);
        }
       
        return sep.substring(0, sep.length() - 1) + right; // fix ending '+'
    }

    private String pad(String text, int length) {
        StringBuilder result = new StringBuilder(text);
        for (int i = text.length(); i < length; i++) {
            result.append(' ');
        }
        return result.toString();
    }

    private static final Map<Class<?>, List<Field>> COLUMN_FIELD_CACHE = new IdentityHashMap<>();

    private static List<java.lang.reflect.Field> columnFields(Class<?> clazz) {
        return COLUMN_FIELD_CACHE.computeIfAbsent(clazz, (c) -> {
            return Arrays.stream(c.getDeclaredFields())
               .filter(f -> f.isAnnotationPresent(Column.class))
               .collect(java.util.stream.Collectors.toList());
        });
    }

  
    private final static Map<List<Field>, List<String>> HEADERS_CACHE = new IdentityHashMap<>();

    private static List<String> headers(List<Field> fields) {
        return HEADERS_CACHE.computeIfAbsent(fields, (fs) -> {
            return fs.stream()
              .map(f -> {
                  String value = f.getAnnotation(Column.class).value();
                  if (value == null || value.isEmpty()) return f.getName();
                  return value;
              })
              .collect(java.util.stream.Collectors.toList());
        });
    }
}