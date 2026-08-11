export type RepairStatus = 'RECEIVED' | 'IN_PROGRESS' | 'READY_TO_DELIVER' | 'DELIVERED';
export type RepairPaymentStatus = 'UNPAID' | 'PARTIAL' | 'PAID';

export interface Repair {
  id: string;
  clientFirstName: string;
  clientLastName: string;
  clientPhone: string;
  itemDescription: string;
  repairDescription: string;
  photoUrls?: string[];
  entryDate: string;
  deliveryDate?: string;
  repairStatus: RepairStatus;
  paymentStatus: RepairPaymentStatus;
  totalPrice: number;
  paymentAmount?: number;
  balanceDue?: number;
  organizationId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRepairRequest {
  clientFirstName: string;
  clientLastName: string;
  clientPhone: string;
  itemDescription: string;
  repairDescription: string;
  totalPrice: number;
  entryDate?: string;
  deliveryDate?: string;
  photoUrls?: string[];
}

export interface RepairPayment {
  id: string;
  repairId: string;
  amount: number;
  paymentDate: string;
  paymentMethod?: string;
  notes?: string;
  createdAt: string;
}

export interface AddRepairPaymentRequest {
  amount: number;
  paymentDate: string;
  paymentMethod?: string;
  notes?: string;
}

export interface UpdateRepairStatusRequest {
  repairStatus: RepairStatus;
}
