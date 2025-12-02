package bg.energo.phoenix.service.signing.qes;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QesSocketSender {

    private final SimpMessagingTemplate messagingTemplate;

    public void convertAndSend(String dest,Object payload){
        Map<String,Object> header=new HashMap<>();
        header.put("auto-delete","true");
        messagingTemplate.convertAndSend(dest,payload,header);
    }
}
