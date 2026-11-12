"""Cuenta líneas físicas de código Kotlin; excluye blancos, comentarios, package e imports.
La tabla principal compara solo los adaptadores de Activity. Los auxiliares se
reportan aparte y no se presentan como totales equivalentes entre librerías.
"""
import json
import re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/melvinpena/comparativa'

def count(text):
    total=0
    in_comment=False
    for line in text.splitlines():
        value=line.strip()
        if in_comment:
            if '*/' in value: in_comment=False
            continue
        if value.startswith('/*'):
            in_comment='*/' not in value
            continue
        if value and not value.startswith(('//','import ','package ')): total+=1
    return total

def adapter(text,name):
    match=re.search(r'^    private fun '+name+r'\(',text,re.M)
    assert match
    end=re.search(r'^    (?:private fun |override fun )',text[match.end():],re.M)
    assert end
    return text[match.start():match.end()+end.start()]

if __name__=='__main__':
    source=(BASE/'ComparativaActivity.kt').read_text()
    result={'criterio':'Líneas físicas no vacías; excluye comentarios completos, package e imports; incluye llaves y firmas. UI excluida.',
        'adaptadores_activity':{c:count(adapter(source,'requestWith'+c)) for c in ['Volley','OkHttp','Retrofit']},
        'auxiliar_okhttp_httpClient':count(adapter(source,'httpClient')),
        'archivos_retrofit_compartidos_completos':{name:count((BASE/name).read_text()) for name in [
            'data/remote/RegistroApi.kt','data/remote/ApiClient.kt','data/repository/RegistroRepository.kt']}}
    output=ROOT/'docs/evidencias/laboratorio-lineas.json'
    output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(result,ensure_ascii=False,indent=2))
