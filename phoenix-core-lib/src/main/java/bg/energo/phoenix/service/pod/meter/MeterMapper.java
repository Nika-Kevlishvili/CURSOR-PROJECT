package bg.energo.phoenix.service.pod.meter;

import bg.energo.phoenix.model.entity.pod.meter.Meter;
import bg.energo.phoenix.model.entity.pod.meter.MeterScale;
import bg.energo.phoenix.model.enums.pod.PodSubObjectStatus;
import bg.energo.phoenix.model.enums.pod.meter.MeterStatus;
import bg.energo.phoenix.model.request.pod.meter.MeterRequest;
import org.springframework.stereotype.Service;

@Service
public class MeterMapper {

    public Meter fromRequestToMeterEntity(MeterRequest request) {
        Meter meter = new Meter();
        meter.setNumber(request.getNumber());
        meter.setGridOperatorId(request.getGridOperatorId());
        meter.setInstallmentDate(request.getInstallmentDate());
        meter.setRemoveDate(request.getRemoveDate());
        meter.setPodId(request.getPodId());
        meter.setStatus(MeterStatus.ACTIVE);
        return meter;
    }
    

    public MeterScale fromRequestToMeterScaleEntity(Long meterId, Long scaleId) {
        MeterScale meterScale = new MeterScale();
        meterScale.setMeterId(meterId);
        meterScale.setScaleId(scaleId);
        meterScale.setStatus(PodSubObjectStatus.ACTIVE);
        return meterScale;
    }
    
    
    public Meter updateMeterFromRequest(MeterRequest request, Meter meter) {
        meter.setPodId(request.getPodId());
        meter.setGridOperatorId(request.getGridOperatorId());
        meter.setInstallmentDate(request.getInstallmentDate());
        meter.setRemoveDate(request.getRemoveDate());
        return meter;
    }

}
