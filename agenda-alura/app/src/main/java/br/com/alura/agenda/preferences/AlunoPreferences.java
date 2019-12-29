package br.com.alura.agenda.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class AlunoPreferences {

    private static final String ALUNO_PREFERENCES = "br.com.alura.agenda.preferences.AlunoPreferences";
    private static final String VERSAO_DO_DADO = "versão_do_dado";
    private Context context;

    public AlunoPreferences(Context context) {
        this.context = context;
    }

    private SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences(ALUNO_PREFERENCES, context.MODE_PRIVATE);
    }

    public String getVersao() {
        SharedPreferences preferences = getSharedPreferences();
        return preferences.getString(VERSAO_DO_DADO, "");
    }

    public void salvaVersao(String versao) {
        SharedPreferences preferences = context.getSharedPreferences(ALUNO_PREFERENCES, context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(VERSAO_DO_DADO, versao);
        editor.commit();
    }

    public boolean temVersao() {
        return !getVersao().isEmpty();

    }
}