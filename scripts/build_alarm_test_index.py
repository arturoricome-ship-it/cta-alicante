from pathlib import Path

src = Path('index.html').read_text(encoding='utf-8')
marker='<!-- CTA_ALARMAS_PRUEBA_LOCAL_V1 -->'
if marker in src:
    raise SystemExit('El panel de prueba ya existe')

panel = r'''
<!-- CTA_ALARMAS_PRUEBA_LOCAL_V1 -->
<style>
#ctaAlarmTestBtn{position:fixed;right:14px;bottom:78px;z-index:2147483000;width:48px;height:48px;border:0;border-radius:50%;background:#f59e0b;color:#111827;font-size:23px;box-shadow:0 4px 18px #0006}
#ctaAlarmTestModal{display:none;position:fixed;inset:0;z-index:2147483001;background:#0009;align-items:center;justify-content:center;padding:18px;font-family:system-ui,-apple-system,sans-serif}
#ctaAlarmTestModal.open{display:flex}
.cta-alarm-card{width:min(420px,100%);background:#111827;color:#fff;border:1px solid #374151;border-radius:18px;padding:18px;box-shadow:0 18px 50px #0009}
.cta-alarm-card h3{margin:0 0 5px;font-size:19px}.cta-alarm-card p{margin:0 0 15px;color:#cbd5e1;font-size:13px;line-height:1.35}
.cta-alarm-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}.cta-alarm-field{display:flex;flex-direction:column;gap:5px;margin-bottom:11px}.cta-alarm-field label{font-size:12px;color:#cbd5e1}.cta-alarm-field input{box-sizing:border-box;width:100%;border:1px solid #475569;border-radius:10px;background:#0f172a;color:#fff;padding:10px;font-size:16px}
#ctaAlarmPreview{background:#0f172a;border-radius:12px;padding:12px;margin:6px 0 13px;text-align:center}.cta-alarm-time{font-size:27px;font-weight:800;color:#fbbf24}.cta-alarm-date{font-size:12px;color:#cbd5e1;margin-top:2px}
.cta-alarm-actions{display:grid;gap:9px}.cta-alarm-actions button{border:0;border-radius:11px;padding:12px 10px;font-weight:750;font-size:14px}.cta-clock{background:#e5e7eb;color:#111827}.cta-exact{background:#f59e0b;color:#111827}.cta-close{background:#334155;color:#fff}.cta-alarm-note{font-size:11px;color:#94a3b8;margin-top:10px;line-height:1.35}
</style>
<button id="ctaAlarmTestBtn" type="button" aria-label="Probar alarma">⏰</button>
<div id="ctaAlarmTestModal" role="dialog" aria-modal="true">
  <div class="cta-alarm-card">
    <h3>⏰ Prueba alarma CTA</h3>
    <p>Prueba temporal sobre la web CTA. No modifica turnos ni calendario.</p>
    <div class="cta-alarm-grid">
      <div class="cta-alarm-field"><label for="ctaAlarmDate">Día del turno</label><input id="ctaAlarmDate" type="date"></div>
      <div class="cta-alarm-field"><label for="ctaAlarmEntry">Hora de entrada</label><input id="ctaAlarmEntry" type="time" value="05:00"></div>
    </div>
    <div class="cta-alarm-field"><label for="ctaAlarmOffset">Despertarme minutos antes</label><input id="ctaAlarmOffset" type="number" min="0" max="720" step="5" value="70"></div>
    <div id="ctaAlarmPreview"><div class="cta-alarm-time">--:--</div><div class="cta-alarm-date">Selecciona fecha y hora</div></div>
    <div class="cta-alarm-actions">
      <button class="cta-clock" id="ctaAlarmClock" type="button">Abrir Reloj Android</button>
      <button class="cta-exact" id="ctaAlarmExact" type="button">CTA exacta · APK</button>
      <button class="cta-close" id="ctaAlarmClose" type="button">Cerrar</button>
    </div>
    <div class="cta-alarm-note">Reloj Android prepara una alarma por hora/minutos. CTA exacta envía también la fecha a la APK CTA Alarma ya instalada.</div>
  </div>
</div>
<script>
(function(){
  const $=id=>document.getElementById(id), btn=$('ctaAlarmTestBtn'), modal=$('ctaAlarmTestModal'), date=$('ctaAlarmDate'), entry=$('ctaAlarmEntry'), offset=$('ctaAlarmOffset'), preview=$('ctaAlarmPreview');
  if(!btn||!modal)return;
  const pad=n=>String(n).padStart(2,'0');
  function today(){const d=new Date();return d.getFullYear()+'-'+pad(d.getMonth()+1)+'-'+pad(d.getDate())}
  date.value=today();
  offset.value=localStorage.getItem('cta_alarm_offset_min')||'70';
  function calc(){
    const ds=date.value, ts=entry.value; if(!ds||!ts)return null;
    const start=new Date(ds+'T'+ts+':00'); if(Number.isNaN(start.getTime()))return null;
    let mins=parseInt(offset.value,10); if(!Number.isFinite(mins)||mins<0)mins=70;
    localStorage.setItem('cta_alarm_offset_min',String(mins));
    const alarm=new Date(start.getTime()-mins*60000);
    preview.innerHTML='<div class="cta-alarm-time">'+pad(alarm.getHours())+':'+pad(alarm.getMinutes())+'</div><div class="cta-alarm-date">'+alarm.toLocaleDateString('es-ES',{weekday:'long',day:'2-digit',month:'2-digit',year:'numeric'})+' · entrada '+ts+'</div>';
    return {start,alarm,entry:ts,mins};
  }
  function openModal(){modal.classList.add('open');calc()}
  btn.addEventListener('click',openModal); $('ctaAlarmClose').addEventListener('click',()=>modal.classList.remove('open'));
  modal.addEventListener('click',e=>{if(e.target===modal)modal.classList.remove('open')});
  [date,entry,offset].forEach(el=>el.addEventListener('input',calc));
  $('ctaAlarmClock').addEventListener('click',function(){
    const c=calc(); if(!c)return alert('Completa fecha y hora.');
    const h=c.alarm.getHours(), m=c.alarm.getMinutes(), label=encodeURIComponent('CTA · Entrada '+c.entry);
    const intent='intent:#Intent;action=android.intent.action.SET_ALARM;i.android.intent.extra.alarm.HOUR='+h+';i.android.intent.extra.alarm.MINUTES='+m+';S.android.intent.extra.alarm.MESSAGE='+label+';B.android.intent.extra.alarm.SKIP_UI=false;end';
    location.href=intent;
  });
  $('ctaAlarmExact').addEventListener('click',function(){
    const c=calc(); if(!c)return alert('Completa fecha y hora.');
    if(c.alarm.getTime()<=Date.now())return alert('La hora calculada ya ha pasado. Elige una fecha/hora futura.');
    const label=encodeURIComponent('CTA · Entrada '+c.entry);
    location.href='ctaalarm://set?at='+c.alarm.getTime()+'&label='+label;
  });
  calc();
})();
</script>
'''

pos=src.lower().rfind('</body>')
if pos < 0:
    raise SystemExit('No se encontró </body>')
out=src[:pos]+panel+'\n'+src[pos:]
Path('index-cta-alarmas-prueba.html').write_text(out,encoding='utf-8')
Path('/tmp/cta-alarm-test.js').write_text(panel.split('<script>',1)[1].split('</script>',1)[0],encoding='utf-8')
print('Generado index-cta-alarmas-prueba.html')
