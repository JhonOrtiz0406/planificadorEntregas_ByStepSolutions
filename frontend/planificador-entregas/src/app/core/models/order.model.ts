export type ProgressStatus = string;
export type PaymentStatus = 'UNPAID' | 'PARTIAL' | 'PAID';

export interface Order {
  id: string;
  orderNumber: string;
  productName: string;
  clientName: string;
  clientPhone?: string;
  clientAddress?: string;
  description?: string;
  photoUrl?: string;
  deliveryDate: string;
  progressStatus: ProgressStatus;
  paymentStatus: PaymentStatus;
  paymentAmount?: number;
  totalPrice?: number;
  balanceDue?: number;
  organizationId: string;
  daysUntilDelivery: number;
  overdue: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrderRequest {
  productName: string;
  clientName: string;
  clientPhone?: string;
  clientAddress?: string;
  description?: string;
  photoUrl?: string;
  deliveryDate: string;
  totalPrice?: number;
}

export interface UpdateOrderStatusRequest {
  progressStatus?: ProgressStatus;
  paymentStatus?: PaymentStatus;
  paymentAmount?: number;
}

export interface CalendarEvent {
  id: string;
  title: string;
  date: string;
  backgroundColor: string;
  borderColor: string;
  textColor: string;
  extendedProps: Order;
}
