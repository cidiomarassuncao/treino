package br.treino.nutricao;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(TnBiometricPlugin.class); // digital para abrir o app
        super.onCreate(savedInstanceState);
    }
}
