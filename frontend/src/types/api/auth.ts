export interface AuthUserDto {
  id: number;
  email: string;
  displayName: string;
  role: string;
}

export interface LoginBody {
  email: string;
  password: string;
}

export interface RegisterBody {
  email: string;
  displayName: string;
  password: string;
}
