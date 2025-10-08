package ar.com.appcontrol.POJO;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ar.com.appcontrol.R;

import java.util.ArrayList;

public class PuestoAdapter extends ArrayAdapter {

    public PuestoAdapter(Context context, ArrayList<PuestoDM> listaPuestos){
        super(context,0,listaPuestos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = initViewWithDivider(position, convertView, parent);

        // Si es el último elemento, ocultar la línea divisoria
        if (position == getCount() - 1) {
            View divider = view.findViewById(R.id.divider);
            if (divider != null) {
                divider.setVisibility(View.GONE);
            }
        }

        return view;
    }

    private View initView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.spinner_item_puestos, parent, false);
        }

        TextView textViewNombrePuesto = convertView.findViewById(R.id.textViewNombrePuesto);
        TextView textViewHorario = convertView.findViewById(R.id.textViewHorario);

        PuestoDM puesto = (PuestoDM) getItem(position);

        if(puesto != null){
            if(puesto.getPUES_DHOR() != null && puesto.getPUES_HHOR() != null){
                String nombrePuesto = puesto.getPUES_NOMB();
                textViewNombrePuesto.setText(nombrePuesto);
                String horario = puesto.getPUES_DHOR() + " a " + puesto.getPUES_HHOR();
                textViewHorario.setText(horario);
                textViewHorario.setVisibility(View.VISIBLE);
            } else {
                textViewNombrePuesto.setText(puesto.getPUES_NOMB());
                textViewHorario.setVisibility(View.GONE);
            }

        }

        return convertView;
    }

    private View initViewWithDivider(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.spinner_item_with_divider, parent, false);
        }

        TextView textViewNombrePuesto = convertView.findViewById(R.id.textViewNombrePuesto);
        TextView textViewHorario = convertView.findViewById(R.id.textViewHorario);

        PuestoDM puesto = (PuestoDM) getItem(position);

        if(puesto != null){
            if(puesto.getPUES_DHOR() != null && puesto.getPUES_HHOR() != null){
                String nombrePuesto = puesto.getPUES_NOMB();
                textViewNombrePuesto.setText(nombrePuesto);
                String horario = puesto.getPUES_DHOR() + " a " + puesto.getPUES_HHOR();
                textViewHorario.setText(horario);
                textViewHorario.setVisibility(View.VISIBLE);
            } else {
                textViewNombrePuesto.setText(puesto.getPUES_NOMB());
                textViewHorario.setVisibility(View.GONE);
            }

        }

        return convertView;
    }


}
