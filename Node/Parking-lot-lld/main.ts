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
    ) {}
}

class Car extends Vehicle {
    constructor(licensePlate: string){
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
    private vehicle? : Vehicle;

    constructor(
        public readonly id: string,
        public readonly floorId: string,
        public readonly type: SpotType 
    ) {}

    isAvailable(): boolean {
        return this.vehicle === undefined;
    }

    park(vechile: Vehicle): void {
        if(!this.isAvailable()){
            throw new Error(`Spot ${this.id} is already occupied`);
        }
        this.vehicle = vechile;
    }

    release(): void {
        if(this.isAvailable()){
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
    ) {}

    addSpot(spot: ParkingSpot): void {
        if(this.spots.has(spot.id)){
            throw new Error(`Spot ${spot.id} already exists`)
        }
        this.spots.set(spot.id, spot)
    }

    getSpot(id: string): ParkingSpot | undefined {
        return this.spots.get(id);
    }


}