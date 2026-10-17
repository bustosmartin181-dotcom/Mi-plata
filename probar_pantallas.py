"""Prueba de pantallas de Mi plata en un navegador real (Chromium sin ventana, vía Playwright).
Uso:  python3 probar_pantallas.py      (en la carpeta donde está index.html)
Requiere:  pip install playwright  y  playwright install chromium
Comprueba: orden de tarjetas en Hoy, objetivo en Días, que con el teclado abierto (se simula achicando la
ventana) el campo Monto y el botón azul se ven juntos, que la barra de abajo se esconde y vuelve, y que se
puede anotar un gasto. Los 'errores_js' de red (ERR_NAME_NOT_RESOLVED) son fuentes de internet y son normales sin conexión.
NO prueba Capacitor, el widget ni las notificaciones (solo existen dentro de la app instalada)."""
import json, pathlib, sys
from playwright.sync_api import sync_playwright
estado={"sueldo":420000,"previo":0,"bienvenida":True,"ingresos":[{"cat":"Sueldo","a":420000}],
 "start":"2026-10-01","end":"2026-10-31","items":[{"n":"Alquiler","a":150000,"p":True}],
 "spent":[{"id":1,"date":"2026-10-16","amt":12000,"cat":"Comida","note":"Almuerzo"},{"id":2,"date":"2026-10-17","amt":2000,"cat":"Transporte","note":"Colectivo"}],
 "recal":None,"objetivo":10000}
resultado={}
with sync_playwright() as p:
    b=p.chromium.launch()
    ctx=b.new_context(viewport={'width':411,'height':860},device_scale_factor=2,is_mobile=True,has_touch=True)
    pg=ctx.new_page(); errores=[]
    pg.on('pageerror',lambda e:errores.append(str(e)))
    pg.on('console',lambda m: errores.append('console:'+m.text) if m.type=='error' else None)
    pg.goto((pathlib.Path(__file__).parent / 'index.html').resolve().as_uri())
    pg.evaluate("e=>localStorage.setItem('miplata_v2',JSON.stringify(e))",estado)
    pg.reload(); pg.wait_for_timeout(700)
    # --- Hoy
    titulos=pg.eval_on_selector_all('#m .card h2, #m .card',"els=>els.map(e=>e.querySelector? (e.querySelector('h2')?e.querySelector('h2').textContent:'(card sin título)'):e.textContent)")
    resultado['hoy_tarjetas']=pg.eval_on_selector_all('#m .card',"els=>els.map(e=>(e.querySelector('h2')||{}).textContent||e.textContent.slice(0,30))")
    resultado['objetivo_en_hoy']=pg.locator('#m', has_text='Mi objetivo por día').count()>0
    pg.screenshot(path='/tmp/hoy_sin_teclado.png')
    # --- botón + (FAB) y teclado simulado (la ventana se achica como cuando sale el teclado de Android)
    pg.click('nav [data-t=dias]'); pg.wait_for_timeout(300)
    resultado['dias_tarjetas']=pg.eval_on_selector_all('#m .card',"els=>els.map(e=>(e.querySelector('h2')||{}).textContent||e.textContent.slice(0,30))")
    pg.screenshot(path='/tmp/dias.png')
    pg.click('#fab'); pg.wait_for_timeout(200)
    pg.set_viewport_size({'width':411,'height':520}); pg.wait_for_timeout(1300)
    resultado['nav_oculta_con_teclado']=pg.evaluate("getComputedStyle(document.querySelector('nav')).display==='none'")
    resultado['campo_monto_enfocado']=pg.evaluate("document.activeElement&&document.activeElement.id")
    caja=pg.evaluate("(()=>{const b=document.querySelector('#tarjeta-anotar [data-a=anotar]').getBoundingClientRect();const a=document.querySelector('#am').getBoundingClientRect();const t=document.querySelector('#tarjeta-anotar').getBoundingClientRect();return {boton_abajo:b.bottom,boton_arriba:b.top,monto_arriba:a.top,tarjeta_arriba:t.top,tarjeta_abajo:t.bottom,alto_ventana:innerHeight}})()")
    resultado['posiciones_con_teclado']=caja
    resultado['boton_y_monto_visibles_juntos']=caja['monto_arriba']>=0 and caja['boton_abajo']<=caja['alto_ventana']
    pg.screenshot(path='/tmp/hoy_con_teclado.png')
    # --- al salir del campo vuelve la barra
    pg.set_viewport_size({'width':411,'height':860}); pg.evaluate("document.activeElement.blur()"); pg.wait_for_timeout(600)
    resultado['nav_vuelve']=pg.evaluate("getComputedStyle(document.querySelector('nav')).display!=='none'")
    # --- anotar un gasto de verdad desde el formulario
    pg.fill('#am','3500'); pg.fill('#nt','Prueba'); pg.click('#tarjeta-anotar [data-a=anotar]'); pg.wait_for_timeout(500)
    resultado['gasto_anotado']=pg.locator('#m', has_text='Prueba').count()>0
    resultado['errores_js']=errores
    b.close()
print(json.dumps(resultado,ensure_ascii=False,indent=1))
