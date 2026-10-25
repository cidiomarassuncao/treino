package br.treino.nutricao;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.pm.PackageInfoCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.Executor;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Digital para abrir o app.
 * Cria uma chave dentro do cofre do Android (Keystore) que só pode ser usada depois de a digital ser
 * confirmada, a cada uso. O app guarda a chave mestra dos dados cifrada por essa chave: sem a digital,
 * o texto guardado no celular não abre.
 */
@CapacitorPlugin(name = "TnBiometric")
public class TnBiometricPlugin extends Plugin {

    private static final String ALIAS = "tn_bio_key_v1";
    private static final String TRANSFORMACAO = "AES/GCM/NoPadding";

    private interface Concluir {
        void ok(Cipher cipher) throws Exception;
    }

    /** Número da versão instalada do app (o mesmo do aviso de atualização). */
    @PluginMethod
    public void appVersion(PluginCall call) {
        try {
            PackageInfo info = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            JSObject o = new JSObject();
            o.put("codigo", (int) PackageInfoCompat.getLongVersionCode(info));
            o.put("nome", info.versionName);
            call.resolve(o);
        } catch (Exception e) {
            call.reject("Não consegui ler a versão do app.", "ERRO_VERSAO");
        }
    }

    /** Grava a cópia dos dados num arquivo temporário e abre a lista de apps (OneDrive, Drive, e-mail...). */
    @PluginMethod
    public void compartilhar(PluginCall call) {
        String nome = call.getString("nome", "copia.json");
        String dados = call.getString("dados");
        if (dados == null || dados.isEmpty()) {
            call.reject("Sem dados para guardar.", "SEM_DADOS");
            return;
        }
        try {
            File dir = new File(getContext().getCacheDir(), "copias");
            dir.mkdirs();
            File[] antigos = dir.listFiles();
            if (antigos != null) for (File a : antigos) a.delete();
            File arq = new File(dir, nome.replaceAll("[^A-Za-z0-9._-]", "_"));
            FileOutputStream out = new FileOutputStream(arq);
            out.write(dados.getBytes(StandardCharsets.UTF_8));
            out.close();
            Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", arq);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("application/json");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_SUBJECT, "Cópia do app de treino");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getActivity().startActivity(Intent.createChooser(i, "Guardar a cópia em…"));
            call.resolve();
        } catch (Exception e) {
            call.reject("Não consegui preparar a cópia.", "ERRO_COPIA");
        }
    }

    @PluginMethod
    public void isAvailable(PluginCall call) {
        int r = BiometricManager.from(getContext()).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        JSObject o = new JSObject();
        o.put("disponivel", r == BiometricManager.BIOMETRIC_SUCCESS);
        o.put("codigo", r);
        call.resolve(o);
    }

    /** Recebe a chave mestra (base64), pede a digital e devolve "iv:cifrado" (base64). */
    @PluginMethod
    public void encrypt(final PluginCall call) {
        final String dados = call.getString("dados");
        if (dados == null || dados.isEmpty()) {
            call.reject("Faltam os dados para guardar.", "SEM_DADOS");
            return;
        }
        try {
            removerChave();
            SecretKey chave = criarChave();
            Cipher cipher = Cipher.getInstance(TRANSFORMACAO);
            cipher.init(Cipher.ENCRYPT_MODE, chave);
            pedirDigital(call, cipher, call.getString("titulo", "Ativar a digital"),
                    call.getString("subtitulo", "Confirme para guardar a chave do app."), new Concluir() {
                        @Override
                        public void ok(Cipher c) throws Exception {
                            byte[] iv = c.getIV();
                            byte[] cifrado = c.doFinal(Base64.decode(dados, Base64.NO_WRAP));
                            JSObject o = new JSObject();
                            o.put("blob", Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(cifrado, Base64.NO_WRAP));
                            call.resolve(o);
                        }
                    });
        } catch (Exception e) {
            call.reject("Não foi possível preparar a digital: " + e.getMessage(), "ERRO_PREPARO");
        }
    }

    /** Recebe "iv:cifrado", pede a digital e devolve a chave mestra (base64). */
    @PluginMethod
    public void decrypt(final PluginCall call) {
        final String blob = call.getString("blob");
        if (blob == null || !blob.contains(":")) {
            call.reject("Não há chave guardada com a digital.", "KEY_MISSING");
            return;
        }
        try {
            String[] partes = blob.split(":");
            byte[] iv = Base64.decode(partes[0], Base64.NO_WRAP);
            final byte[] cifrado = Base64.decode(partes[1], Base64.NO_WRAP);
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            SecretKey chave = (SecretKey) ks.getKey(ALIAS, null);
            if (chave == null) {
                call.reject("A chave da digital não existe neste aparelho.", "KEY_MISSING");
                return;
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMACAO);
            cipher.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(128, iv));
            pedirDigital(call, cipher, call.getString("titulo", "Entrar no app"),
                    call.getString("subtitulo", "Confirme com a digital."), new Concluir() {
                        @Override
                        public void ok(Cipher c) throws Exception {
                            JSObject o = new JSObject();
                            o.put("dados", Base64.encodeToString(c.doFinal(cifrado), Base64.NO_WRAP));
                            call.resolve(o);
                        }
                    });
        } catch (KeyPermanentlyInvalidatedException e) {
            removerChave();
            call.reject("A digital cadastrada no aparelho mudou.", "KEY_INVALIDATED");
        } catch (Exception e) {
            call.reject("Não foi possível usar a digital: " + e.getMessage(), "ERRO_PREPARO");
        }
    }

    @PluginMethod
    public void remove(PluginCall call) {
        removerChave();
        call.resolve();
    }

    private SecretKey criarChave() throws Exception {
        KeyGenerator gerador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec.Builder b = new KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true) // a chave só funciona depois da digital
                .setInvalidatedByBiometricEnrollment(true); // digital nova no aparelho invalida a chave
        if (Build.VERSION.SDK_INT >= 30) {
            b.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG); // digital a cada uso
        }
        gerador.init(b.build());
        return gerador.generateKey();
    }

    private void removerChave() {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS);
        } catch (Exception ignorar) {
        }
    }

    private void pedirDigital(final PluginCall call, final Cipher cipher, final String titulo,
                              final String subtitulo, final Concluir fim) {
        final AppCompatActivity act = getActivity();
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Executor executor = ContextCompat.getMainExecutor(act);
                    BiometricPrompt prompt = new BiometricPrompt(act, executor, new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult resultado) {
                            try {
                                fim.ok(resultado.getCryptoObject().getCipher());
                            } catch (Exception e) {
                                call.reject("Falha ao usar a chave: " + e.getMessage(), "ERRO_CRIPTO");
                            }
                        }

                        @Override
                        public void onAuthenticationError(int codigo, CharSequence mensagem) {
                            call.reject(String.valueOf(mensagem), codigoErro(codigo));
                        }
                    });
                    BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                            .setTitle(titulo)
                            .setSubtitle(subtitulo)
                            .setNegativeButtonText("Usar a senha")
                            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            .setConfirmationRequired(false)
                            .build();
                    prompt.authenticate(info, new BiometricPrompt.CryptoObject(cipher));
                } catch (Exception e) {
                    call.reject("Não foi possível abrir a digital: " + e.getMessage(), "ERRO_PREPARO");
                }
            }
        });
    }

    private static String codigoErro(int codigo) {
        if (codigo == BiometricPrompt.ERROR_USER_CANCELED || codigo == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                || codigo == BiometricPrompt.ERROR_CANCELED) return "CANCELADO";
        if (codigo == BiometricPrompt.ERROR_LOCKOUT || codigo == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) return "BLOQUEADO";
        return "ERRO_" + codigo;
    }
}
