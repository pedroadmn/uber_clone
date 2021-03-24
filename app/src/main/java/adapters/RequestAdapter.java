package adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import helpers.Local;
import models.Request;
import models.User;
import pedroadmn.uberclone.com.R;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.MyViewHolder> {

    private List<Request> requests;
    private Context context;
    private User driver;

    public RequestAdapter(List<Request> requests, Context context, User driver) {
        this.requests = requests;
        this.context = context;
        this.driver = driver;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.request_adapter, parent, false);
        return new MyViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int i) {
        Request request = requests.get(i);
        User passenger = request.getPassenger();

        holder.name.setText(request.getPassenger().getName());

        if (driver != null) {
            LatLng passengerLocation = new LatLng(
                    Double.parseDouble(passenger.getLatitude()),
                    Double.parseDouble(passenger.getLongitude()));

            LatLng driverLocation = new LatLng(
                    Double.parseDouble(passenger.getLatitude()),
                    Double.parseDouble(passenger.getLongitude())
            );

            float distance = Local.calculateDistance(passengerLocation, driverLocation);
            String formattedDistance = Local.formattedDistance(distance);
            holder.distance.setText(formattedDistance + " approximately");
        }


//        holder.companyName.setText(company.getName());
//        holder.category.setText(company.getCategory() + " - ");
//        holder.time.setText(company.getTime() + " Min");
//        holder.delivery.setText("R$ " + company.getTax());
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        TextView distance;

        public MyViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.tvRequestName);
            distance = itemView.findViewById(R.id.tvRequestDistance);
        }
    }
}
