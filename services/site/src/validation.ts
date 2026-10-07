import type { Rule } from 'antd/es/form';

const INT32_MIN = -2147483648;
const INT32_MAX = 2147483647;

export const requiredRule = (message: string): Rule => ({ required: true, message });

export const notBlankRule = (message: string): Rule => ({ required: true, whitespace: true, message });

export const integerRule = (message = 'Введите целое число'): Rule => ({ type: 'integer', message });

export const int32Rule = (message = 'Слишком большое по модулю число'): Rule => ({
  type: 'number',
  min: INT32_MIN,
  max: INT32_MAX,
  message,
});

export const minRule = (min: number, message: string): Rule => ({ type: 'number', min, message });

export const positiveRule = (message: string): Rule => ({
  validator: (_, value) => (value == null || value > 0 ? Promise.resolve() : Promise.reject(new Error(message))),
});

export const lengthRule = (min: number, max: number, message: string): Rule => ({ type: 'string', min, max, message });

export const notOnlySpacesRule = (message: string): Rule => ({ type: 'string', whitespace: true, message });
