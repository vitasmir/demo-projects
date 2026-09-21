import { NextRequest, NextResponse } from "next/server";

type Fruit = { id: number; name: string; color: string; weight: number };
type FruitInput = Omit<Fruit, "id">;

let fruits: Fruit[] = [
  { id: 1, name: "Jablko", color: "Červená", weight: 180 },
  { id: 2, name: "Banán", color: "Žlutá", weight: 120 },
  { id: 3, name: "Švestka", color: "Fialová", weight: 65 },
  { id: 4, name: "Pomeranč", color: "Oranžová", weight: 210 },
];

function isFruitInput(value: unknown): value is FruitInput {
  if (value == null || typeof value !== "object") return false;

  const input = value as Record<string, unknown>;
  return (
    typeof input.name === "string" &&
    input.name.trim() !== "" &&
    typeof input.color === "string" &&
    input.color.trim() !== "" &&
    typeof input.weight === "number" &&
    Number.isFinite(input.weight) &&
    input.weight > 0
  );
}

export function GET() {
  return NextResponse.json(fruits);
}

export async function POST(request: NextRequest) {
  const input: unknown = await request.json();
  if (!isFruitInput(input)) {
    return NextResponse.json({ error: "Neplatné údaje ovoce." }, { status: 400 });
  }

  const fruit: Fruit = {
    id: Date.now(),
    name: input.name.trim(),
    color: input.color.trim(),
    weight: input.weight,
  };
  fruits = [...fruits, fruit];
  return NextResponse.json(fruit, { status: 201 });
}

export async function PUT(request: NextRequest) {
  const body: unknown = await request.json();
  if (body == null || typeof body !== "object") {
    return NextResponse.json({ error: "Neplatné údaje ovoce." }, { status: 400 });
  }

  const input = body as Record<string, unknown>;
  const id = input.id;
  if (typeof id !== "number" || !isFruitInput(input)) {
    return NextResponse.json({ error: "Neplatné údaje ovoce." }, { status: 400 });
  }

  const updatedFruit: Fruit = {
    id,
    name: input.name.trim(),
    color: input.color.trim(),
    weight: input.weight,
  };
  if (!fruits.some((fruit) => fruit.id === id)) {
    return NextResponse.json({ error: "Ovoce nebylo nalezeno." }, { status: 404 });
  }

  fruits = fruits.map((fruit) => (fruit.id === id ? updatedFruit : fruit));
  return NextResponse.json(updatedFruit);
}

export async function DELETE(request: NextRequest) {
  const id = Number(new URL(request.url).searchParams.get("id"));
  if (!Number.isInteger(id)) {
    return NextResponse.json({ error: "Neplatné ID ovoce." }, { status: 400 });
  }

  const previousLength = fruits.length;
  fruits = fruits.filter((fruit) => fruit.id !== id);
  if (fruits.length === previousLength) {
    return NextResponse.json({ error: "Ovoce nebylo nalezeno." }, { status: 404 });
  }

  return new NextResponse(null, { status: 204 });
}