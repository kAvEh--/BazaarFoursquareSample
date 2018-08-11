package kaveh.bazaarfoursquaresample;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import kaveh.bazaarfoursquaresample.Model.Venue;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
    private List<Venue> venues;
    private MainActivity activity;

    ListAdapter(List<Venue> myDataset, MainActivity act) {
        venues = myDataset;
        this.activity = act;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView txtName;
        private TextView txtCategory;
        private TextView txtDistance;
        public View layout;

        private ViewHolder(View v) {
            super(v);
            layout = v;
            txtName = v.findViewById(R.id.name);
            txtCategory = v.findViewById(R.id.category);
            txtDistance = v.findViewById(R.id.distance);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            activity.recyclerViewListClicked(v, this.getLayoutPosition());
        }
    }

    @Override
    public ListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.list_item_layout, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.txtName.setText(venues.get(position).getName());
        holder.txtCategory.setText(venues.get(position).getCategories().get(0).getShortName());
        holder.txtDistance.setText(String.valueOf(venues.get(position).getLocation().getDistance()) + "m");
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return venues.size();
    }
}