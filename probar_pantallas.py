"""Prueba de pantallas de Mi plata en un navegador real (Chromium sin ventana, vía Playwright).

Uso:  python3 probar_pantallas.py      (en la carpeta donde está index.html)
Requiere:  pip install playwright  y  playwright install chromium

Comprueba, con fechas relativas a hoy (no depende del día en que se corra):
  1. Hoy: "Te queda para hoy" y justo debajo "Anotar gasto"; el objetivo NO está en Hoy sino en Días.
  2. Con el teclado abierto (se simula achicando la ventana) el campo Monto y el botón azul se ven juntos,
     la barra de abajo se esconde y vuelve, y se puede anotar un gasto.
  3. Objetivo: escribir un número + Enter cambia el límite oficial de hoy; borrarlo + Enter vuelve a lo normal;
     un objetivo viejo sin aplicar muestra el aviso y el botón; "Recalibrar" saca el objetivo.
NO prueba Capacitor, el widget ni las notificaciones (solo existen dentro de la app instalada).
Los errores de red (ERR_NAME_NOT_RESOLVED) son fuentes de internet y son normales sin conexión."""
import datetime, json, pathlib, re, sys
from playwright.sync_api import sync_playwright

URL = (pathlib.Path(__file__).parent / 'index.html').resolve().as_uri()
hoy = datetime.date.today(); ayer = hoy - datetime.timedelta(days=1)
inicio = hoy - datetime.timedelta(days=16); fin = inicio + datetime.timedelta(days=30)
ESTADO = {"sueldo": 420000, "previo": 0, "bienvenida": True, "ingresos": [{"cat": "Sueldo", "a": 420000}],
          "start": inicio.isoformat(), "end": fin.isoformat(), "items": [{"n": "Alquiler", "a": 150000, "p": True}],
          "spent": [{"id": 1, "date": ayer.isoformat(), "amt": 12000, "cat": "Comida", "note": "Almuerzo"},
                    {"id": 2, "date": hoy.isoformat(), "amt": 2000, "cat": "Transporte", "note": "Colectivo"}],
          "recal": None}
fallas = []
def comprobar(condicion, texto):
    print(('  OK    ' if condicion else '  FALLA ') + texto)
    if not condicion: fallas.append(texto)
def plano(t): return re.sub(r'\s+', ' ', t)

with sync_playwright() as p:
    b = p.chromium.launch()
    ctx = b.new_context(viewport={'width': 411, 'height': 860}, device_scale_factor=2, is_mobile=True, has_touch=True)
    def abrir(estado):
        pg = ctx.new_page(); pg.on('pageerror', lambda e: fallas.append('error de JavaScript: ' + str(e)))
        pg.goto(URL); pg.evaluate("e=>localStorage.setItem('miplata_v2',JSON.stringify(e))", estado); pg.reload(); pg.wait_for_timeout(600)
        return pg
    def hoy_texto(pg):
        pg.click('nav [data-t=hoy]'); pg.wait_for_timeout(250); return plano(pg.locator('#m .card').first.inner_text())
    def en_dias(pg): pg.click('nav [data-t=dias]'); pg.wait_for_timeout(250)

    print('1) Orden de las pantallas')
    pg = abrir({**ESTADO, "objetivo": 0})
    titulos = pg.eval_on_selector_all('#m .card', "els=>els.map(e=>(e.querySelector('h2')||{}).textContent||e.textContent.slice(0,22))")
    comprobar(titulos[0].startswith('Te queda para hoy') and titulos[1] == 'Anotar gasto', 'Hoy: "Te queda para hoy" y justo debajo "Anotar gasto"')
    comprobar('Mi objetivo por día' not in pg.inner_text('#m'), 'El objetivo no está en Hoy')
    en_dias(pg)
    comprobar('Mi objetivo por día' in pg.inner_text('#m'), 'El objetivo está en Días')

    print('2) Teclado simulado y botón azul')
    pg.click('#fab'); pg.wait_for_timeout(200)
    pg.set_viewport_size({'width': 411, 'height': 520}); pg.wait_for_timeout(1300)
    caja = pg.evaluate("(()=>{const b=document.querySelector('#tarjeta-anotar [data-a=anotar]').getBoundingClientRect();const a=document.querySelector('#am').getBoundingClientRect();return {b:b.bottom,a:a.top,h:innerHeight}})()")
    comprobar(caja['a'] >= 0 and caja['b'] <= caja['h'], 'Monto y botón azul se ven juntos con el teclado abierto')
    comprobar(pg.evaluate("getComputedStyle(document.querySelector('nav')).display==='none'"), 'La barra de abajo se esconde con el teclado')
    pg.set_viewport_size({'width': 411, 'height': 860}); pg.evaluate("document.activeElement.blur()"); pg.wait_for_timeout(600)
    comprobar(pg.evaluate("getComputedStyle(document.querySelector('nav')).display!=='none'"), 'La barra vuelve al salir del campo')
    pg.fill('#am', '3500'); pg.fill('#nt', 'Prueba'); pg.click('#tarjeta-anotar [data-a=anotar]'); pg.wait_for_timeout(500)
    comprobar('Prueba' in pg.inner_text('#m'), 'Se puede anotar un gasto')
    pg.close()

    print('3) El objetivo cambia el límite oficial')
    pg = abrir({**ESTADO, "objetivo": 10000})      # objetivo guardado por una versión anterior, sin aplicar
    normal = hoy_texto(pg)
    en_dias(pg)
    comprobar('no es tu límite oficial' in plano(pg.inner_text('#m')), 'Un objetivo viejo sin aplicar avisa que todavía no es el límite')
    pg.click('[data-a=usarObjetivo]'); pg.wait_for_timeout(300)
    t = hoy_texto(pg)
    comprobar('Te queda para hoy $8.000' in t and 'Presupuesto de hoy $10.000' in t, 'El botón "Usar como mi límite" cambia Hoy (10.000 − 2.000 = 8.000)')
    pg.close()
    pg = abrir({**ESTADO, "objetivo": 0}); normal = hoy_texto(pg); en_dias(pg)
    pg.click('[data-f=objetivo]'); pg.keyboard.type('10000'); pg.keyboard.press('Enter'); pg.wait_for_timeout(400)
    comprobar(pg.evaluate("getComputedStyle(document.querySelector('nav')).display!=='none'"), 'Con Enter la barra de abajo no queda escondida')
    t = hoy_texto(pg)
    comprobar('Te queda para hoy $8.000' in t and 'Mañana tendrás $18.000' in t, 'Escribir 10000 + Enter: hoy queda $8.000 y mañana $18.000')
    en_dias(pg); pg.fill('[data-f=objetivo]', ''); pg.keyboard.press('Enter'); pg.wait_for_timeout(400)
    comprobar(hoy_texto(pg) == normal, 'Borrar el número + Enter vuelve al límite normal')
    en_dias(pg); pg.fill('[data-f=objetivo]', '10000'); pg.keyboard.press('Enter'); pg.wait_for_timeout(300)
    pg.click('nav [data-t=hoy]'); pg.wait_for_timeout(200); pg.click('[data-a=recalibrar]'); pg.wait_for_timeout(300)
    est = pg.evaluate("JSON.parse(localStorage.getItem('miplata_v2'))")
    comprobar(est['objetivo'] == 0 and not (est['recal'] or {}).get('obj'), '"Recalibrar" saca el objetivo (no pueden convivir)')
    b.close()

print('\nRESULTADO:', 'TODO BIEN' if not fallas else 'HAY FALLAS -> ' + '; '.join(fallas))
sys.exit(1 if fallas else 0)
