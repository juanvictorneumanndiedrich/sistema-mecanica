package com.mecanica.util;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Genera los reportes de JasperReports del sistema. Los disenos (.jrxml)
 * estan en src/main/resources/reportes/ y se compilan UNA sola vez por
 * ejecucion (la primera vez que se imprime cada uno), quedando en cache.
 *
 * Los datos siempre llegan como una lista de Map (una fila = un Map, con las
 * claves iguales a los nombres de los field del .jrxml), armada por el
 * ReporteController -- asi el reporte no depende de las entidades de
 * Hibernate ni de sesiones abiertas.
 */
public final class ReporteUtil {

    public static final Locale LOCALE = Locale.of("es", "PY");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Map<String, JasperReport> CACHE = new ConcurrentHashMap<>();

    private ReporteUtil() {
        // clase utilitaria: no debe ser instanciada
    }

    /**
     * Compila (si hace falta) y llena el reporte. Los parametros comunes del
     * encabezado (nombre/datos del taller, fecha de emision, idioma) se
     * agregan aca automaticamente.
     *
     * @param nombre     nombre del archivo en /reportes, sin ".jrxml"
     * @param titulo     titulo impreso arriba a la derecha
     * @param subtitulo  linea debajo del titulo (puede ser null)
     * @param parametros parametros propios de ese reporte (puede ser null)
     * @param filas      una fila por Map; lista vacia imprime el reporte sin detalle
     */
    public static JasperPrint generar(String nombre, String titulo, String subtitulo,
                                      Map<String, Object> parametros,
                                      List<? extends Map<String, ?>> filas) throws JRException {
        JasperReport reporte = compilar(nombre);

        Map<String, Object> todos = new HashMap<>();
        if (parametros != null) {
            todos.putAll(parametros);
        }
        todos.put("TALLER_NOMBRE", DatosTaller.NOMBRE);
        todos.put("TALLER_DATOS", DatosTaller.lineaDatos());
        todos.put("TITULO", titulo);
        todos.put("SUBTITULO", subtitulo);
        todos.put("FECHA_EMISION", LocalDateTime.now().format(FORMATO_FECHA_HORA));
        todos.put(JRParameter.REPORT_LOCALE, LOCALE);

        @SuppressWarnings("unchecked")
        Collection<Map<String, ?>> datos = (Collection<Map<String, ?>>) (Collection<?>) filas;
        return JasperFillManager.fillReport(reporte, todos, new JRMapCollectionDataSource(datos));
    }

    private static JasperReport compilar(String nombre) throws JRException {
        JasperReport enCache = CACHE.get(nombre);
        if (enCache != null) {
            return enCache;
        }
        String ruta = "/reportes/" + nombre + ".jrxml";
        try (InputStream diseno = ReporteUtil.class.getResourceAsStream(ruta)) {
            if (diseno == null) {
                throw new JRException("No se encontro el diseno del reporte: " + ruta);
            }
            JasperReport compilado = JasperCompileManager.compileReport(diseno);
            CACHE.put(nombre, compilado);
            return compilado;
        } catch (java.io.IOException e) {
            throw new JRException("No fue posible leer el diseno del reporte: " + ruta, e);
        }
    }

    // ---------------------------------------------------------------- Ayudas de formato

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FORMATO_FECHA);
    }

    public static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    public static BigDecimal valor(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    /** "EN_PROCESO" -> "En proceso", "VALE_SEMANAL" -> "Vale semanal". */
    public static String enumLegible(Enum<?> valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.name().replace('_', ' ').toLowerCase(LOCALE);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
