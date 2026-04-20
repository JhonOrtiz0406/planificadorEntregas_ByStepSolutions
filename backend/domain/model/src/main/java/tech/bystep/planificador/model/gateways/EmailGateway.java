package tech.bystep.planificador.model.gateways;

public interface EmailGateway {

    /** Sends an invitation email with the acceptance link. */
    void sendInvitation(String to, String token, String organizationName);

    /** Notifies the ORG_ADMIN that their organization was disabled/deleted. */
    void sendOrgClosedToAdmin(String to, String orgName, boolean deleted);

    /** Notifies an ORG_EMPLOYEE or ORG_DELIVERY that their org access was revoked. */
    void sendOrgClosedToMember(String to, String orgName, String adminEmail, boolean deleted);

    /** Notifies the ORG_ADMIN that their organization was reactivated. */
    void sendOrgReactivated(String to, String orgName);

    /** Forwards a support contact form to the platform support email. */
    void sendSupportContact(String userName, String userEmail, String phone,
                            String organizationName, String message);
}
