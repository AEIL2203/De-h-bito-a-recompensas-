const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

async function enviarATokens(query, titulo, cuerpo, data, filtro = () => true, tag = null) {
  const snapshot = await query.get();
  const docs = snapshot.docs.filter(doc => doc.data().token && filtro(doc.data()));
  if (!docs.length) return;
  const resultado = await getMessaging().sendEachForMulticast({
    tokens: docs.map(doc => doc.data().token),
    notification: { title: titulo, body: cuerpo },
    android: {
      priority: "high",
      notification: { channelId: data.canal || "uso_apps", ...(tag ? { tag } : {}) }
    },
    data
  });
  const invalidos = [];
  resultado.responses.forEach((respuesta, i) => {
    if (!respuesta.success && ["messaging/invalid-registration-token", "messaging/registration-token-not-registered"].includes(respuesta.error?.code)) invalidos.push(docs[i].ref.delete());
  });
  await Promise.all(invalidos);
}

async function enviarATutor(tutorId, titulo, cuerpo, data, tag = null) {
  await enviarATokens(getFirestore().collection("tokens_fcm").where("tutorId", "==", tutorId), titulo, cuerpo, data, token => token.modo !== "hijo", tag);
}

async function enviarANino(ninoId, titulo, cuerpo, data, tag = null) {
  await enviarATokens(getFirestore().collection("tokens_fcm").where("ninoId", "==", ninoId), titulo, cuerpo, data, () => true, tag);
}

exports.sesionAppIniciada = onDocumentCreated("sesiones_apps/{sesionId}", async event => {
  const sesion = event.data?.data();
  if (!sesion || sesion.estado !== "iniciada") return;
  await enviarATutor(sesion.tutorId, `${sesion.nombre} iniciada`, "El niño ha comenzado a usar la aplicación", { sesionId: event.params.sesionId, ninoId: sesion.ninoId, estado: "iniciada", canal: "uso_apps" });
});

exports.sesionAppFinalizada = onDocumentUpdated("sesiones_apps/{sesionId}", async event => {
  const antes = event.data?.before.data();
  const despues = event.data?.after.data();
  if (!antes || !despues || antes.estado === "finalizada" || despues.estado !== "finalizada") return;
  const minutos = Math.max(0, Math.round((despues.duracionMs || 0) / 60000));
  await enviarATutor(despues.tutorId, `${despues.nombre} finalizada`, `Tiempo usado: ${minutos} min`, { sesionId: event.params.sesionId, ninoId: despues.ninoId, estado: "finalizada", canal: "uso_apps" });
});

exports.tareaCreada = onDocumentCreated("tareas/{tareaId}", async event => {
  const tarea = event.data?.data();
  if (!tarea || tarea.estado !== "pendiente") return;
  await enviarANino(tarea.ninoId, "Nueva misión", `Tienes una nueva misión: ${tarea.nombre} (+${tarea.puntos} XP)`, { tareaId: event.params.tareaId, ninoId: tarea.ninoId, tipo: "nueva", canal: "tareas" });
});

exports.tareaActualizada = onDocumentUpdated("tareas/{tareaId}", async event => {
  const antes = event.data?.before.data();
  const despues = event.data?.after.data();
  if (!antes || !despues || antes.estado === despues.estado) return;
  const data = { tareaId: event.params.tareaId, ninoId: despues.ninoId, canal: "tareas" };
  if (antes.estado === "pendiente" && despues.estado === "completada") {
    const ninoDoc = await getFirestore().collection("ninos").doc(despues.ninoId).get();
    const nombreNino = ninoDoc.data()?.nombre || "Tu hijo";
    await enviarATutor(despues.tutorId, "Misión por validar", `${nombreNino} completó "${despues.nombre}"`, { ...data, tipo: "por_validar" }, "por_validar_" + despues.tutorId);
  } else if (antes.estado === "completada" && despues.estado === "validada") {
    const hoy = new Date().toISOString().slice(0, 10);
    const validadas = await getFirestore().collection("tareas")
      .where("ninoId", "==", despues.ninoId)
      .where("estado", "==", "validada")
      .get();
    const totalHoy = validadas.docs.filter(doc => (doc.data().fechaCompletada || "").startsWith(hoy)).length;
    let titulo = "¡Misión validada!";
    let cuerpo = `Ganaste +${despues.puntos} XP por "${despues.nombre}"`;
    if (totalHoy > 1) {
      titulo = "¡Misiones validadas!";
      cuerpo = `Se validaron todas tus misiones (${totalHoy} en total)`;
    }
    await enviarANino(despues.ninoId, titulo, cuerpo, { ...data, tipo: "validada", agrupar: "validadas" }, "validadas_" + despues.ninoId);
  } else if (antes.estado === "completada" && despues.estado === "pendiente") {
    await enviarANino(despues.ninoId, "Misión devuelta", `"${despues.nombre}" necesita otro intento`, { ...data, tipo: "devuelta" });
  }
});

exports.recompensaCanjeada = onDocumentCreated("recompensas/{recompensaId}", async event => {
  const recompensa = event.data?.data();
  if (!recompensa || recompensa.minutosOtorgados == null) return;
  const ninoDoc = await getFirestore().collection("ninos").doc(recompensa.ninoId).get();
  const nombreNino = ninoDoc.data()?.nombre || "Tu hijo";
  const appName = ninoDoc.data()?.apps?.[recompensa.packageName] || recompensa.packageName;
  await enviarATutor(
    recompensa.tutorId,
    "Tiempo canjeado",
    `${nombreNino} canjeó ${recompensa.minutosOtorgados} minutos para ${appName}`,
    { recompensaId: event.params.recompensaId, ninoId: recompensa.ninoId, tipo: "canje", canal: "recompensas" },
    "canje_" + recompensa.tutorId
  );
});
