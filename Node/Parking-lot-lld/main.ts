// parking-lot -> parking floor -> parking spot -> vechile -> ticket -> payment

// parking lot has floor 1, 2, and 3 
// parking lot 1-* parking floor

// class ParkingLot {
//     floors: ParkingFloor[];
// }

// parking floor 1-* parking spot 
// class ParkingFloor {
//     spots: ParkingSpot[];
// }

// vechile -> Motorcycle, car ,truck
// abstract class Vechile {
//     licenseplate: string;
//     type: VechileType;
// }

// class Car extends Vechile {};
// class Truck extends Vechile {};
// class Motorcycle extends Vechile {};

// Parking spot types
// enum SpotType {
//     MOTORCYCLE,
//     COMPACT,
//     LARGE,
// }

// class ParkingSopt {
//     id: string;
//     type: SpotType;
// }

// Ticket relationship
// vechile -> parkingspot -> ticket 
// ticket -> ticketid, vechile, parkingspot, entryTime, exittime, status 
// class Parkingticket {
//     id: string;
//     vechile: Vechile;
//     spot: ParkingSopt;
//     entryTime: Date;
//     exitTime: Date;
// } 

// payment 
// class Payment {
//     id: string;
//     ticketId: string;
//     amount: number;
//     method: PaymentMethod;
//     status: PaymentStatus;
//     paidAt: Date;
// }




enum VehicleType {
    MOTORCYCLE = 'MOTORCYCLE',
    CAR = 'CAR',
    TRUCK = 'TRUCK',
}

enum SpotType {
    MOTORCYCLE = 'MOTORCYCLE',
    COMPACT = 'COMPACT',
    LARGE = 'LARGE',
}

enum TicketStatus {
    ACTIVE = 'ACTIVE',
    COMPLETED = 'COMPLETED',
}

enum PaymentStatus {
    PENDING = 'PENDING',
    SUCCESS = 'SUCCESS',
    FAILED = 'FAILED',
}

abstract class Vehicle {
    constructor(
        public readonly licensePlate: string,
        public readonly type: VehicleType,
    ) { }
}

class Car extends Vehicle {
    constructor(licensePlate: string) {
        super(licensePlate, VehicleType.CAR)
    }
}

class Motorcycle extends Vehicle {
    constructor(licensePlate: string) {
        super(licensePlate, VehicleType.MOTORCYCLE);
    }
}

class Truck extends Vehicle {
    constructor(licensePlate: string) {
        super(licensePlate, VehicleType.TRUCK);
    }
}

// const car = new Car('BR01AB1234');

// console.log(car.licensePlate);
// BR01AB1234

// console.log(car.type);
// CAR 

class ParkingSpot {
    private vehicle?: Vehicle;

    constructor(
        public readonly id: string,
        public readonly floorId: string,
        public readonly type: SpotType
    ) { }

    isAvailable(): boolean {
        return this.vehicle === undefined;
    }

    park(vechile: Vehicle): void {
        if (!this.isAvailable()) {
            throw new Error(`Spot ${this.id} is already occupied`);
        }
        this.vehicle = vechile;
    }

    release(): void {
        if (this.isAvailable()) {
            throw new Error(`Spot ${this.id} is already empty`);
        }
        this.vehicle = undefined;
    }

    getVechile(): Vehicle | undefined {
        return this.vehicle;
    }
}


class ParkingFloor {
    private readonly spots = new Map<string, ParkingSpot>();

    constructor(
        public readonly id: string
    ) { }

    addSpot(spot: ParkingSpot): void {
        if (this.spots.has(spot.id)) {
            throw new Error(`Spot ${spot.id} already exists`)
        }
        this.spots.set(spot.id, spot)
    }

    getSpot(id: string): ParkingSpot | undefined {
        return this.spots.get(id);
    }

    getSpots(): ParkingSpot[] {
        return [...this.spots.values()];
    }

    getAvailableSpots(): ParkingSpot[] {
        return this.getSpots().filter(
            spot => spot.isAvailable(),
        )
    }
}


class ParkingTicket {
    public readonly entryTime: Date;

    private exitTime?: Date;
    private status: TicketStatus = TicketStatus.ACTIVE;

    constructor(
        public readonly id: string,
        public readonly vehicle: Vehicle,
        public readonly spot: ParkingSpot,
    ) {
        this.entryTime = new Date()
    }

    close(exitTime: Date = new Date()): void {
        if (this.status !== TicketStatus.ACTIVE) {
            throw new Error(
                'Ticket is already completed',
            );
        }

        this.exitTime = exitTime;
        this.status = TicketStatus.COMPLETED;
    }
}

class ParkingLot {
  constructor(
    public readonly id: string,
    private readonly floors: ParkingFloor[],
  ) {}

  addFloor(floor: ParkingFloor): void {
    this.floors.push(floor);
  }

  getFloors(): ParkingFloor[] {
    return [...this.floors];
  }
}

const floor1 = new ParkingFloor('F1');

const compactSpot =
  new ParkingSpot(
    'C1',
    'F1',
    SpotType.COMPACT,
  );

const largeSpot =
  new ParkingSpot(
    'L1',
    'F1',
    SpotType.LARGE,
  );

floor1.addSpot(compactSpot);
floor1.addSpot(largeSpot);

const parkingLot =
  new ParkingLot(
    'PL1',
    [floor1],
  );

const car =
  new Car('BR01AB1234');

compactSpot.park(car);

const ticket =
  new ParkingTicket(
    'TICKET-001',
    car,
    compactSpot,
  );


