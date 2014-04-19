package com.doplgangr.secrecy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class VaultAdapter extends BaseAdapter {
    // store the context (as an inflated layout)
    private LayoutInflater inflater;
    // store the resource (typically file_item.xml)
    private int resource;
    // store (a reference to) the data
    private ArrayList<String> data;
    private ArrayList<Integer> checked = new ArrayList<Integer>();

    /**
     * Default constructor. Creates the new Adaptor object to
     * provide a ListView with data.
     *
     * @param context
     * @param resource
     * @param data
     */
    public VaultAdapter(Context context, int resource, ArrayList<String> data) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.resource = resource;
        this.data = data;
    }

    /**
     * Return the size of the data set.
     */
    public int getCount() {
        return this.data.size();
    }

    /**
     * Return an object in the data set.
     */
    public String getItem(int position) {
        return this.data.get(position);
    }

    /**
     * Return the position provided.
     */
    public long getItemId(int position) {
        return position;
    }

    /**
     * Return a generated view for a position.
     */
    public void update(ArrayList<String> data) {
        this.data = data;
        checked.clear();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        // reuse a given view, or inflate a new one from the xml
        View view;

        if (convertView == null) {
            view = this.inflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        // bind the data to the view object
        return this.bindData(view, position);
    }

    /**
     * Bind the provided data to the view.
     * This is the only method not required by base adapter.
     */
    public View bindData(View view, int position) {
        // make sure it's worth drawing the view
        if (this.data.get(position) == null) {
            return view;
        }

        // pull out the object
        String vault = this.data.get(position);

        // extract the view object
        View viewElement = view.findViewById(R.id.name);
        // cast to the correct type
        TextView tv = (TextView) viewElement;
        // set the value
        tv.setText(vault);
        // return the final view object
        return view;
    }

    public Boolean select(int position) {
        if (checked.contains(position))
            checked.remove(checked.indexOf(position));
        else
            checked.add(position);
        return checked.contains(position);
    }

    public ArrayList<Integer> getSelected() {
        return checked;
    }

    public void clearSelected() {
        checked.clear();
    }

    public void clear() {
        data.clear();
    }
}
