export const FormState = {
  Creating: 'creating',
  Updating: 'updating',
} as const;

export type FormState = (typeof FormState)[keyof typeof FormState];
