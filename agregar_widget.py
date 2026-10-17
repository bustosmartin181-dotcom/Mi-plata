#!/usr/bin/env python3
"""Agrega el widget "Mi plata - Hoy" al proyecto Android que crea Capacitor.

Lo ejecuta el workflow de GitHub (despues de "npx cap add android").
Escribe los archivos del widget, registra el widget en el AndroidManifest y
reemplaza MainActivity para que el boton "+" del widget abra "anotar gasto".
"""
import re
import sys
from pathlib import Path

RAIZ = Path("android/app/src/main")
MANIFIESTO = RAIZ / "AndroidManifest.xml"

ARCHIVOS = {
    "res/layout/widget_miplata.xml": r'''<?xml version="1.0" encoding="utf-8"?>
<!-- Widget "Mi plata — Hoy": cuánto te queda hoy + botón para anotar un gasto -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center_vertical"
    android:padding="14dp"
    android:background="@drawable/widget_background">

    <TextView
        android:id="@+id/widget_titulo"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Me queda hoy"
        android:textSize="13sp"
        android:textColor="@color/widget_texto_suave"
        android:fontFamily="sans-serif-medium" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="2dp"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <TextView
            android:id="@+id/widget_monto"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="$ —"
            android:textSize="28sp"
            android:textStyle="bold"
            android:textColor="@color/widget_verde"
            android:fontFamily="sans-serif-medium"
            android:maxLines="1"
            android:ellipsize="end" />

        <ImageButton
            android:id="@+id/widget_btn_anotar"
            android:layout_width="42dp"
            android:layout_height="42dp"
            android:src="@drawable/ic_add"
            android:background="@drawable/widget_btn_circle"
            android:contentDescription="Anotar gasto"
            android:scaleType="centerInside"
            android:padding="9dp" />
    </LinearLayout>

    <TextView
        android:id="@+id/widget_secundario"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="2dp"
        android:text=""
        android:textSize="12sp"
        android:textColor="@color/widget_texto_tenue"
        android:maxLines="1"
        android:ellipsize="end" />

    <TextView
        android:id="@+id/widget_objetivo"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text=""
        android:textSize="12sp"
        android:textColor="@color/widget_texto_tenue"
        android:maxLines="1"
        android:ellipsize="end" />
</LinearLayout>
''',

    "res/xml/widget_miplata_info.xml": r'''<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="180dp"
    android:minHeight="90dp"
    android:targetCellWidth="3"
    android:targetCellHeight="2"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/widget_miplata"
    android:previewImage="@drawable/widget_preview"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:description="@string/widget_description" />
''',

    "res/values/strings_widget.xml": r'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="widget_description">Muestra cuánto te queda para gastar hoy</string>
    <string name="widget_name">Mi plata — Hoy</string>
</resources>
''',

    "res/values/colors_widget.xml": r'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="widget_fondo">#FFFFFF</color>
    <color name="widget_borde">#E5DFD6</color>
    <color name="widget_verde">#1B6B4A</color>
    <color name="widget_texto_suave">#6B756F</color>
    <color name="widget_texto_tenue">#8A958E</color>
</resources>
''',

    "res/values-night/colors_widget.xml": r'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="widget_fondo">#1F2622</color>
    <color name="widget_borde">#33403A</color>
    <color name="widget_verde">#5FD3A1</color>
    <color name="widget_texto_suave">#A6B3AB</color>
    <color name="widget_texto_tenue">#8C9992</color>
</resources>
''',

    "res/drawable/widget_background.xml": r'''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/widget_fondo" />
    <corners android:radius="20dp" />
    <stroke android:width="1dp" android:color="@color/widget_borde" />
</shape>
''',

    "res/drawable/widget_btn_circle.xml": r'''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#1B6B4A" />
</shape>
''',

    "res/drawable/ic_add.xml": r'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
</vector>
''',

    "res/drawable/widget_preview.xml": r'''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFF" />
            <corners android:radius="20dp" />
            <stroke android:width="1dp" android:color="#E5DFD6" />
        </shape>
    </item>
    <item android:gravity="center" android:width="56dp" android:height="56dp">
        <layer-list>
            <item>
                <shape android:shape="oval">
                    <solid android:color="#1B6B4A" />
                </shape>
            </item>
            <item android:drawable="@drawable/ic_add" android:left="12dp" android:top="12dp" android:right="12dp" android:bottom="12dp" />
        </layer-list>
    </item>
</layer-list>
''',

    "java/MiPlataWidgetProvider.java": r'''package __PAQUETE__;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Widget "Mi plata - Hoy". Lee widget.json (lo escribe la app) y muestra cuanto queda hoy.
 * El boton "+" abre la app lista para anotar un gasto.
 */
public class MiPlataWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        actualizar(context, manager, ids);
    }

    /** Tambien la usa MainActivity para refrescar el widget al salir de la app. */
    public static void actualizar(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null) return;
        for (int id : ids) {
            manager.updateAppWidget(id, armarVista(context));
        }
    }

    private static String leerArchivo(File archivo) throws Exception {
        BufferedReader lector = new BufferedReader(new InputStreamReader(new FileInputStream(archivo), "UTF-8"));
        try {
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = lector.readLine()) != null) sb.append(linea);
            return sb.toString();
        } finally {
            lector.close();
        }
    }

    private static String plata(NumberFormat nf, long n) {
        return (n < 0 ? "-" : "") + "$" + nf.format(Math.abs(n));
    }

    private static RemoteViews armarVista(Context context) {
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_miplata);

        boolean noche = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int verde = Color.parseColor(noche ? "#5FD3A1" : "#1B6B4A");
        int naranja = Color.parseColor(noche ? "#F2A65A" : "#B86E1E");
        int rojo = Color.parseColor(noche ? "#FF7A7A" : "#C23B3B");
        int gris = Color.parseColor(noche ? "#A6B3AB" : "#6B756F");

        String monto = "$ \u2014";
        String secundario = "Abr\u00ed la app para empezar";
        String objetivo = "";
        int colorMonto = verde;

        try {
            File archivo = new File(context.getFilesDir(), "widget.json");
            if (archivo.exists()) {
                JSONObject j = new JSONObject(leerArchivo(archivo));
                NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "AR"));
                nf.setMaximumFractionDigits(0);

                long queda = j.optLong("quedaHoy", 0);
                long gasto = j.optLong("gastoHoy", 0);
                long meta = j.optLong("objetivo", 0);
                long sobra = j.optLong("objetivoSobra", 0);
                String color = j.optString("color", "ok");
                boolean oculto = j.optBoolean("oculto", false);
                String fecha = j.optString("fecha", "");
                String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                boolean viejo = !fecha.equals(hoy);

                monto = oculto ? "\u2022\u2022\u2022\u2022\u2022\u2022" : plata(nf, queda);
                if ("neg".equals(color)) colorMonto = rojo;
                else if ("warn".equals(color)) colorMonto = naranja;

                if (viejo) {
                    colorMonto = gris;
                    secundario = "Dato de otro d\u00eda \u00b7 abr\u00ed la app";
                } else if (oculto) {
                    secundario = "Montos ocultos";
                } else {
                    secundario = gasto > 0 ? "Gastado hoy: " + plata(nf, gasto) : "Todav\u00eda no gastaste hoy";
                    if (meta > 0) {
                        objetivo = "Objetivo " + plata(nf, meta) + "/d\u00eda \u00b7 "
                                + (sobra >= 0 ? "sobran " + plata(nf, sobra) : "faltan " + plata(nf, -sobra));
                    }
                }
            }
        } catch (Exception ignorada) {
            // sin archivo o con error: queda el texto por defecto
        }

        v.setTextViewText(R.id.widget_monto, monto);
        v.setTextColor(R.id.widget_monto, colorMonto);
        v.setTextViewText(R.id.widget_secundario, secundario);
        v.setTextViewText(R.id.widget_objetivo, objetivo);

        int banderas = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        // Tocar el widget: abre la app
        Intent abrir = new Intent(context, MainActivity.class);
        abrir.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 0, abrir, banderas));

        // Tocar el "+": abre la app lista para anotar un gasto
        Intent anotar = new Intent(context, MainActivity.class);
        anotar.setAction("__PAQUETE__.ACTION_ANOTAR");
        anotar.putExtra("miplata_action", "anotar");
        anotar.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        v.setOnClickPendingIntent(R.id.widget_btn_anotar, PendingIntent.getActivity(context, 1, anotar, banderas));

        return v;
    }
}
''',

    "java/MainActivity.java": r'''package __PAQUETE__;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.getcapacitor.BridgeActivity;

/**
 * Actividad principal. Ademas de lo normal de Capacitor:
 *  - si el widget toco "+", le avisa a la pagina para que abra "anotar gasto";
 *  - refresca el widget al salir de la app.
 */
public class MainActivity extends BridgeActivity {

    private String accionPendiente = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        leerAccion(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        leerAccion(intent);
    }

    private void leerAccion(Intent intent) {
        if (intent == null) return;
        String accion = intent.getStringExtra("miplata_action");
        if ("anotar".equals(accion)) {
            accionPendiente = accion;
            intent.removeExtra("miplata_action");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (accionPendiente != null) {
            final String accion = accionPendiente;
            final long id = System.currentTimeMillis();
            accionPendiente = null;
            Handler handler = new Handler(Looper.getMainLooper());
            // La pagina puede tardar en cargar si la app estaba cerrada: se reintenta (la pagina ignora repetidos).
            int[] demoras = {300, 1200, 2800};
            for (int demora : demoras) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        enviarAccion(accion, id);
                    }
                }, demora);
            }
        }
        refrescarWidgets();
    }

    @Override
    public void onPause() {
        super.onPause();
        refrescarWidgets();
    }

    private void enviarAccion(String accion, long id) {
        try {
            if (getBridge() == null || getBridge().getWebView() == null) return;
            String js = "try{if(window.miplataAbrir)window.miplataAbrir('" + accion + "','" + id + "');}catch(e){}";
            getBridge().getWebView().evaluateJavascript(js, null);
        } catch (Exception ignorada) {
        }
    }

    private void refrescarWidgets() {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            int[] ids = manager.getAppWidgetIds(new ComponentName(this, MiPlataWidgetProvider.class));
            if (ids != null && ids.length > 0) MiPlataWidgetProvider.actualizar(this, manager, ids);
        } catch (Exception ignorada) {
        }
    }
}
''',

}


def main():
    if not MANIFIESTO.exists():
        sys.exit("No encuentro android/app/src/main/AndroidManifest.xml (se corrio 'npx cap add android'?)")

    # Paquete real de la app: sale del MainActivity que genera Capacitor
    candidatos = list((RAIZ / "java").rglob("MainActivity.java"))
    if not candidatos:
        sys.exit("No encuentro MainActivity.java en android/app/src/main/java")
    principal = candidatos[0]
    paquete = re.search(r"^package\s+([\w.]+);", principal.read_text(encoding="utf-8"), re.M).group(1)
    carpeta_java = principal.parent
    print("Paquete de la app:", paquete)

    for ruta, texto in ARCHIVOS.items():
        texto = texto.replace("__PAQUETE__", paquete)
        if ruta.startswith("java/"):
            destino = carpeta_java / Path(ruta).name
        else:
            destino = RAIZ / ruta
        destino.parent.mkdir(parents=True, exist_ok=True)
        destino.write_text(texto, encoding="utf-8")
        print("  escrito:", destino)

    # Registrar el widget en el AndroidManifest (una sola vez)
    manifiesto = MANIFIESTO.read_text(encoding="utf-8")
    if "MiPlataWidgetProvider" not in manifiesto:
        receptor = f"""
        <!-- Widget Mi plata -->
        <receiver
            android:name="{paquete}.MiPlataWidgetProvider"
            android:exported="true"
            android:label="@string/widget_name">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/widget_miplata_info" />
        </receiver>
    """
        if "</application>" not in manifiesto:
            sys.exit("El AndroidManifest no tiene </application>")
        manifiesto = manifiesto.replace("</application>", receptor + "</application>", 1)
        MANIFIESTO.write_text(manifiesto, encoding="utf-8")
        print("AndroidManifest actualizado")
    else:
        print("El AndroidManifest ya tenia el widget")

    print("Widget agregado correctamente")


main()
