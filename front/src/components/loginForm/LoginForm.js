import React,  { useState } from 'react'

import Field from '../field/Field'

const LoginForm = () => {
	const [login, setLogin] = useState('')
	const [password, setPassword] = useState('')


	const actionLogin = () => {
		console.log({ login, password })
	}

	return <div className='form'>
		<Field type='text' placeholder='Введите логин' onChange={setLogin} />
		<Field type='password' placeholder='Введите пароль' onChange={setPassword} />
		<button onClick={actionLogin}>Войти</button>
	</div>
}

export default LoginForm