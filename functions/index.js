const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

async function enviar(tutorId, titulo, cuerpo, data) {
  const snapshot = await getFirestore().collection("tokens_fcm").where("tutorId", "==", tutorId).get();
  const tokens = snapshot.docs.map(doc => doc.data().token).filter(Boolean);
  if (!tokens.length) return;
  const resultado = await getMessaging().sendEachForMulticast({ tokens, notification: { title: titulo, body: cuerpo }, data });
  const invalidos = [];
  resultado.responses.forEach((respuesta, i) => {
    if (!respuesta.success && ["messaging/invalid-registration-token", "messaging/registration-token-not-registered"].includes(respuesta.error?.code)) invalidos.push(snapshot.docs[i].ref.delete());
  });
  await Promise.all(invalidos);
}

exports.sesionAppIniciada = onDocumentCreated("sesiones_apps/{sesionId}", async event => {
  const sesion = event.data?.data();
  if (!sesion || sesion.estado !== "iniciada") return;
  await enviar(sesion.tutorId, `${sesion.nombre} iniciada`, "El niño ha comenzado a usar la aplicación", { sesionId: event.params.sesionId, ninoId: sesion.ninoId, estado: "iniciada" });
});

exports.sesionAppFinalizada = onDocumentUpdated("sesiones_apps/{sesionId}", async event => {
  const antes = event.data?.before.data();
  const despues = event.data?.after.data();
  if (!antes || !despues || antes.estado === "finalizada" || despues.estado !== "finalizada") return;
  const minutos = Math.max(0, Math.round((despues.duracionMs || 0) / 60000));
  await enviar(despues.tutorId, `${despues.nombre} finalizada`, `Tiempo usado: ${minutos} min`, { sesionId: event.params.sesionId, ninoId: despues.ninoId, estado: "finalizada" });
});
