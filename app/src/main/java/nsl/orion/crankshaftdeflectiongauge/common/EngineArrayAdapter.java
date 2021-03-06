package nsl.orion.crankshaftdeflectiongauge.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import nsl.orion.crankshaftdeflectiongauge.R;

/**
 * Created by Hai on 9/17/2015.
 */
public class EngineArrayAdapter extends ArrayAdapter<Engine> {

    public EngineArrayAdapter(Context context, List<Engine> engineList) {
        super(context, 0, engineList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Engine engine = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listviewengine, parent, false);
        }
        TextView engineName = (TextView) convertView.findViewById(R.id.engineName);
        TextView enginedateOfCreation = (TextView) convertView.findViewById(R.id.enginedateOfCreation);
        engineName.setText(engine.getName());

        if (engine.getIsFinished() == Engine.NOT_FINISHED) {
            engineName.setTextColor(getContext().getResources().getColor(R.color.darkpink));
            enginedateOfCreation.setTextColor(getContext().getResources().getColor(R.color.darkpink));
        } else {
            engineName.setTextColor(getContext().getResources().getColor(R.color.cyan));
            enginedateOfCreation.setTextColor(getContext().getResources().getColor(R.color.cyan));
        }
        enginedateOfCreation.setText(engine.getDateOfCreation().toString());

        return convertView;
    }
}

