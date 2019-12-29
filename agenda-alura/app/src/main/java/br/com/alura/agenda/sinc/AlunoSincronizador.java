package br.com.alura.agenda.sinc;

import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import br.com.alura.agenda.dao.AlunoDAO;
import br.com.alura.agenda.dto.AlunosSync;
import br.com.alura.agenda.event.AtualizaListaAlunoEvent;
import br.com.alura.agenda.modelo.Aluno;
import br.com.alura.agenda.preferences.AlunoPreferences;
import br.com.alura.agenda.retrofit.RetrofitInicializador;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlunoSincronizador {

    private final Context context;
    private EventBus bus = EventBus.getDefault();
    private AlunoPreferences preferences;

    public AlunoSincronizador(Context context) {
        this.context = context;
        preferences = new AlunoPreferences(context);

    }

    public void buscaTodos() {
        if (preferences.temVersao()) {
            buscaNovos();
        } else {
            buscaAlunos();
        }
    }

    private void buscaNovos() {
        Call<AlunosSync> call = new RetrofitInicializador().getAlunoService().novos(preferences.getVersao());
        call.enqueue(buscaAlunosCallBack());
    }

    private void buscaAlunos() {
        Call<AlunosSync> call = new RetrofitInicializador().getAlunoService().lista();
        call.enqueue(buscaAlunosCallBack());
    }

    private Callback<AlunosSync> buscaAlunosCallBack() {
        return new Callback<AlunosSync>() {
            @Override
            public void onResponse(Call<AlunosSync> call, Response<AlunosSync> response) {
                AlunosSync alunosSync = response.body();
                sincroniza(alunosSync);

                //Log.i("versao", preferences.getVersao());

                bus.post(new AtualizaListaAlunoEvent());
                sincronizaAlunosInternos();
            }

            @Override
            public void onFailure(Call<AlunosSync> call, Throwable t) {
                Log.e("onFailure chamado", t.getMessage());
                bus.post(new AtualizaListaAlunoEvent());
            }
        };
    }

    public void sincroniza(AlunosSync alunosSync) {
        String versao = alunosSync.getMomentoDaUltimaModificacao();

        Log.i("versao externa", versao);


        if (temVersaoNova(versao)) {
            preferences.salvaVersao(versao);

            Log.i("versao atual", preferences.getVersao());

            AlunoDAO dao = new AlunoDAO(context);
            dao.sincroniza(alunosSync.getAlunos());
            dao.close();
        }
    }

    private boolean temVersaoNova(String versao) {
        if (!preferences.temVersao()) {
            return true;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        try {
            Date dataExterna = format.parse(versao);
            String versaoInterna = preferences.getVersao();

            Log.i("versao interna", versaoInterna);


            Date dataInterna = format.parse(versaoInterna);

            return dataExterna.after(dataInterna);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void sincronizaAlunosInternos() {
        final AlunoDAO dao = new AlunoDAO(context);
        final List<Aluno> alunos = dao.listaNaoSincronizados();

        dao.close();

        Call<AlunosSync> call = new RetrofitInicializador().getAlunoService().atualiza(alunos);

        call.enqueue(new Callback<AlunosSync>() {
            @Override
            public void onResponse(Call<AlunosSync> call, Response<AlunosSync> response) {
                AlunosSync alunosSync = response.body();
                sincroniza(alunosSync);

            }

            @Override
            public void onFailure(Call<AlunosSync> call, Throwable t) {

            }
        });

    }

    public void deleta(final Aluno aluno) {
        Call<Void> call = new RetrofitInicializador().getAlunoService().deleta(aluno.getId());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                AlunoDAO dao = new AlunoDAO(context);
                dao.deleta(aluno);
                dao.close();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });
    }
}