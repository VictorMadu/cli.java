package io.github.victormadu.display;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.victormadu.display.annotation.Column;


public class Display {
   
    public void table(Object o) {
        if (o == null) {
        } else if (o instanceof Throwable) {
            System.out.println(((Throwable) o).getMessage());
        } else if (o instanceof List) {
            System.out.println(table((List<?>) o));
        } else if (o instanceof Iterable) {
            List<Object> list = new ArrayList<>();
            ((Iterable<?>) o).forEach(list::add);
            System.out.println(table(list));
        } else if (o instanceof Object[]) {
            System.out.println(table(Arrays.asList((Object[]) o)));
        } else if (o.getClass().isArray()) {
            int length = Array.getLength(o);
            List<Object> list = new ArrayList<>(length);
            
            for (int i = 0; i < length; i++) {
                list.add(Array.get(o, i));
            }
            
            System.out.println(table(list));
        } else {
            System.out.println(table(Collections.singletonList(o)));
        }
    }

    private String table(List<?> rows) {
        if (rows.isEmpty()) return "";
    
        Class<?> clazz = rows.get(0).getClass();
        List<java.lang.reflect.Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .collect(java.util.stream.Collectors.toList());
    
        // Handle case where the class has no fields (e.g., String, Integer)
        if (fields.isEmpty()) {
            if (rows.size() == 1) {
                return rows.get(0).toString();
            }
    
            // Single-column table with empty header
            int colWidth = rows.stream()
                    .map(r -> r != null ? r.toString().length() : 0)
                    .max(Integer::compare)
                    .orElse(0);
    
            StringBuilder sb = new StringBuilder();
            String horizontal = buildSeparator(new int[]{colWidth}, '+', '-', '+');
    
            sb.append(horizontal).append("\n");
            sb.append(buildRow(Collections.singletonList(""), new int[]{colWidth})).append("\n");
            sb.append(horizontal).append("\n");
    
            for (Object row : rows) {
                String text = row != null ? row.toString() : "";
                sb.append(buildRow(Collections.singletonList(text), new int[]{colWidth})).append("\n");
            }
    
            sb.append(horizontal);
            return sb.toString();
        }
    
        // For normal objects with fields
        List<List<String>> tableData = new ArrayList<>();
        List<String> headers = headers(clazz);
        tableData.add(headers);
    
        for (Object row : rows) {
            List<String> rowData = new ArrayList<>();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(row);
                    if (value instanceof Optional) {
                        value = ((Optional<?>) value).orElse(null);
                    }
                    rowData.add(value != null ? value.toString() : "");
                } catch (IllegalAccessException e) {
                    rowData.add("");
                }
            }
            tableData.add(rowData);
        }
    
        int[] colWidths = new int[headers.size()];
        for (List<String> row : tableData) {
            for (int i = 0; i < row.size(); i++) {
                colWidths[i] = Math.max(colWidths[i], row.get(i).length());
            }
        }
    
        StringBuilder sb = new StringBuilder();
        String horizontal = buildSeparator(colWidths, '+', '-', '+');
    
        sb.append(horizontal).append("\n");
        sb.append(buildRow(tableData.get(0), colWidths)).append("\n");
        sb.append(horizontal).append("\n");
    
        for (int i = 1; i < tableData.size(); i++) {
            sb.append(buildRow(tableData.get(i), colWidths)).append("\n");
        }
    
        sb.append(horizontal);
        return sb.toString();
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

    private final static Map<Class<?>, List<String>> HEADERS_CACHE = new HashMap<>();

    private static List<String> headers(Class<?> clazz) {
        return HEADERS_CACHE.computeIfAbsent(clazz, (c) -> {
            List<java.lang.reflect.Field> fields = Arrays.stream(c.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .collect(java.util.stream.Collectors.toList());
            
            return fields.stream()
                .map(f -> {
                    String value = f.getAnnotation(Column.class).value();
                    if (value == null || value.isEmpty()) return f.getName();
                    return value;
                })
                .collect(java.util.stream.Collectors.toList());
        });
    }
}