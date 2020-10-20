package ru.pekcherkin.androiduberremake.Services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.greenrobot.eventbus.EventBus;

import java.util.Map;
import java.util.Random;

import androidx.annotation.NonNull;
import ru.pekcherkin.androiduberremake.Common;
import ru.pekcherkin.androiduberremake.Model.EventBus.DriverRequestReceived;
import ru.pekcherkin.androiduberremake.Utils.UserUtils;

public class MyFirebaseMessagingService extends FirebaseMessagingService {


    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        if(FirebaseAuth.getInstance().getCurrentUser() != null)
            UserUtils.updateToken(this, s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);



        Map<String, String> dataResv = remoteMessage.getData();
        if(dataResv != null){
            if(dataResv.get(Common.NOTI_TITLE).equals(Common.REQUEST_DRIVER_TITLE)){
                EventBus.getDefault().postSticky(new DriverRequestReceived(
                        dataResv.get(Common.RIDER_KEY),
                        dataResv.get(Common.RIDER_PICKUP_LOCATION)
                ));
            }else{
            Common.showNotification(this, new Random().nextInt(),
                    dataResv.get(Common.NOTI_TITLE),
                    dataResv.get(Common.NOTI_CONTENT),null);
            }
        }
    }
}
