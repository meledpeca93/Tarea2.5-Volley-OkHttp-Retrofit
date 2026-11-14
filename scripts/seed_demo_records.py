"""Agrega tres registros de demostración a la API local sin duplicarlos.
No modifica ni elimina registros existentes; credenciales solo desde archivo local.
"""
import json
import os
import urllib.request
from pathlib import Path
from smoke_android_crud import api_token, records

ROOT=Path(__file__).resolve().parents[1]
DEMOS=[
    ('Demostración: observación de flora','Registro de demostración para el laboratorio HTTP. Observación ficticia de plantas del sendero.'),
    ('Demostración: punto de agua','Registro de demostración para el laboratorio HTTP. Descripción ficticia de un punto de agua.'),
    ('Demostración: recorrido de campo','Registro de demostración para el laboratorio HTTP. Resumen ficticio de un recorrido de campo.'),
]

def main():
    os.chdir(ROOT)
    before=records()
    titles={r['titulo'] for r in before}
    created=[]
    for title,description in DEMOS:
        if title in titles:continue
        request=urllib.request.Request('http://localhost:5000/api/registros',method='POST',
            data=json.dumps({'titulo':title,'descripcion':description,'fotoBase64':None}).encode(),
            headers={'Content-Type':'application/json','Authorization':'Bearer '+api_token()})
        with urllib.request.urlopen(request,timeout=10) as response:
            assert response.status==201
            created.append(json.load(response)['id'])
    after=records()
    assert all(title in {r['titulo'] for r in after} for title,_ in DEMOS)
    assert {r['id'] for r in before}<={r['id'] for r in after}
    print(json.dumps({'antes':len(before),'creados':created,'total':len(after)},ensure_ascii=False))

if __name__=='__main__':main()
