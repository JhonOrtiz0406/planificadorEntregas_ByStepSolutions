package tech.bystep.planificador.model.gateways;

public interface StorageGateway {
    String uploadFile(String fileName, String contentType, byte[] bytes);
}
