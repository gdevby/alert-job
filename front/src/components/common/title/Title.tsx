import './title.scss'

type Props = {
  text: string;
}

const Title = ({text}: Props) => <h1 className='title'>{text}</h1>

export default Title