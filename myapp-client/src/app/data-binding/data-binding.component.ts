import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-data-binding',
  imports: [FormsModule],
  templateUrl: './data-binding.component.html',
  styleUrl: './data-binding.component.scss'
})
export class DataBindingComponent implements OnInit {
  name: string = "Learning databinding";
  topic: string = "Databinding";
  image1: string = "https://plus.unsplash.com/premium_photo-1670210080045-a2e0da63dd99?q=80&w=2030&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D";
  image2: string = "https://www.thoughtco.com/thmb/H3Bt0F4hQuAf9yUQ6bwEEKomDjg=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/child-holding-colorful-gum-balls-576720981-5bfeb5c646e0fb0051b6dc20.jpg";
  image: string = this.image1;

  random = 0.0;
  imageNumber: number = 1;

  generateRandom() {
    this.random = Math.random();
  }

  constructor() {
  }

  ngOnInit(): void {
    this.image = this.image1;
  }
  changeImage() {
    if (this.imageNumber == 1) {
      this.image = this.image2;
      this.imageNumber = 2;
    } else if (this.imageNumber == 2) {
      this.image = this.image1;
      this.imageNumber = 1;
    }
  }
  onSelect(value: string) {
    alert("Country " + value + "  is selected");
  }
  onSave() {
    alert("Data is saved sucessfully");

  }


}